package com.uiu.examintegrity.proctoring.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.course.CourseAccess;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.proctoring.application.view.FacultyExamRow;
import com.uiu.examintegrity.proctoring.application.view.FlagView;
import com.uiu.examintegrity.proctoring.application.view.MonitorView;
import com.uiu.examintegrity.proctoring.application.view.MonitorView.Tile;
import com.uiu.examintegrity.proctoring.application.view.RiskDetailView;
import com.uiu.examintegrity.proctoring.application.view.RiskDetailView.EventView;
import com.uiu.examintegrity.proctoring.domain.RiskTier;
import com.uiu.examintegrity.proctoring.persistence.Flag;
import com.uiu.examintegrity.proctoring.persistence.FlagRepository;
import com.uiu.examintegrity.proctoring.persistence.FlagStatus;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventEntity;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventRepository;
import com.uiu.examintegrity.proctoring.persistence.RiskScore;
import com.uiu.examintegrity.proctoring.persistence.RiskScoreRepository;

@Service
@Transactional(readOnly = true)
public class MonitorService {
    static final Set<FlagStatus> OPEN = EnumSet.of(FlagStatus.RAISED, FlagStatus.UNDER_REVIEW);

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamSessionRepository sessionRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final FlagRepository flagRepository;
    private final ProctoringEventRepository eventRepository;
    private final CourseAccess courseAccess;

    public MonitorService(ExamRepository examRepository, ExamQuestionRepository examQuestionRepository,
            ExamSessionRepository sessionRepository, RiskScoreRepository riskScoreRepository,
            FlagRepository flagRepository, ProctoringEventRepository eventRepository, CourseAccess courseAccess) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.sessionRepository = sessionRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.flagRepository = flagRepository;
        this.eventRepository = eventRepository;
        this.courseAccess = courseAccess;
    }

    public List<FacultyExamRow> exams(Long staffId) {
        List<Long> courses = courseAccess.staffedCourseIds(staffId);
        if (courses.isEmpty()) {
            return List.of();
        }
        return examRepository.findByCourseIdInOrderByStartsAtDesc(courses).stream().map(this::row).toList();
    }

    public FacultyExamRow row(Long examId) {
        return row(examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Not found")));
    }

    private FacultyExamRow row(Exam exam) {
        return new FacultyExamRow(
                exam.getId(), exam.getTitle(), exam.getCourse().getCode(), exam.getStatus().name(),
                exam.getStartsAt(), exam.getEndsAt(), exam.getDurationMinutes(),
                examQuestionRepository.countByExamId(exam.getId()), exam.getQuestionsToDraw(),
                sessionRepository.countByExamId(exam.getId()),
                sessionRepository.countByExamIdAndStatus(exam.getId(), SessionStatus.IN_PROGRESS),
                flagRepository.findBySessionExamIdAndStatusIn(exam.getId(), OPEN).size());
    }

    public MonitorView monitor(Long examId, Long staffId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Not found"));
        courseAccess.requireStaff(exam.getCourse().getId(), staffId);
        return new MonitorView(exam.getId(), exam.getTitle(), exam.getCourse().getCode(), exam.getStatus().name(),
                exam.getStartsAt(), exam.getEndsAt(), exam.getDurationMinutes(), tiles(examId, null));
    }

    public List<Tile> tiles(Long examId, Collection<Long> onlySessions) {
        Map<Long, RiskScore> risks = new HashMap<>();
        riskScoreRepository.findBySessionExamIdOrderByScoreDesc(examId)
                .forEach(risk -> risks.put(risk.getSessionId(), risk));
        Map<Long, String> openFlags = new HashMap<>();
        flagRepository.findBySessionExamIdAndStatusIn(examId, OPEN)
                .forEach(flag -> openFlags.put(flag.getSession().getId(), flag.getStatus().name()));
        Set<Long> wanted = onlySessions == null ? null : new HashSet<>(onlySessions);

        return sessionRepository.findWithUserByExamId(examId).stream()
                .filter(s -> wanted == null || wanted.contains(s.getId()))
                .map(s -> tile(s, risks.get(s.getId()), openFlags.get(s.getId())))
                .sorted((a, b) -> b.score().compareTo(a.score()))
                .toList();
    }

    private static Tile tile(ExamSessionEntity session, RiskScore risk, String flagStatus) {
        BigDecimal score = risk == null ? BigDecimal.ZERO : risk.getScore();
        String tier = risk == null ? RiskTier.CLEAR.name() : risk.getTier().name();
        Map<String, Object> last = risk == null ? null : lastSignal(risk.getBreakdown());
        return new Tile(session.getId(), session.getUser().getFullName(), session.getUser().getStudentId(),
                session.getStatus().name(), session.getStartedAt(), session.getExpiresAt(), score, tier,
                risk == null ? null : risk.getCohortPercentile(),
                last == null ? null : String.valueOf(last.get("detector")),
                last == null || last.get("at") == null ? null : Instant.parse(String.valueOf(last.get("at"))),
                flagStatus);
    }

    private static Map<String, Object> lastSignal(List<Map<String, Object>> breakdown) {
        Map<String, Object> latest = null;
        for (Map<String, Object> row : breakdown) {
            if ("SIGNAL".equals(row.get("kind")) && row.get("at") != null
                    && (latest == null || String.valueOf(row.get("at")).compareTo(String.valueOf(latest.get("at"))) > 0)) {
                latest = row;
            }
        }
        return latest;
    }

    public RiskDetailView riskDetail(Long sessionId, Long staffId) {
        ExamSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Not found"));
        Exam exam = session.getExam();
        courseAccess.requireStaff(exam.getCourse().getId(), staffId);

        RiskScore risk = riskScoreRepository.findById(sessionId).orElse(null);
        List<Map<String, Object>> breakdown = risk == null ? List.of() : risk.getBreakdown();

        Set<Long> evidence = new HashSet<>();
        for (Map<String, Object> row : breakdown) {
            if (row.get("eventIds") instanceof List<?> ids) {
                ids.forEach(id -> evidence.add(((Number) id).longValue()));
            }
        }
        Flag flag = flagRepository.findFirstBySessionIdAndStatusIn(sessionId, OPEN)
                .or(() -> flagRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId))
                .orElse(null);
        if (flag != null) {
            evidence.addAll(flag.getEventIds());
        }

        List<EventView> events = eventRepository.findTop50BySessionIdOrderByOccurredAtDesc(sessionId).stream()
                .map(e -> event(e, evidence))
                .toList();

        return new RiskDetailView(sessionId, exam.getId(), exam.getTitle(), session.getUser().getFullName(),
                session.getUser().getStudentId(), session.getStatus().name(), session.getStartedAt(),
                session.getExpiresAt(), session.getSubmittedAt(),
                risk == null ? BigDecimal.ZERO : risk.getScore(),
                risk == null ? RiskTier.CLEAR.name() : risk.getTier().name(),
                risk == null ? null : risk.getCohortPercentile(),
                risk == null ? null : risk.getUpdatedAt(),
                breakdown, events, flag == null ? null : FlagView.of(flag));
    }

    private static EventView event(ProctoringEventEntity e, Set<Long> evidence) {
        return new EventView(e.getId(), e.getEventType(), e.getOccurredAt(), e.getReceivedAt(),
                e.getQuestion() == null ? null : e.getQuestion().getId(), e.getPayload(), evidence.contains(e.getId()));
    }
}
