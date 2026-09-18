package com.uiu.examintegrity.proctoring.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.common.BadRequestException;
import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.course.CourseAccess;
import com.uiu.examintegrity.proctoring.application.view.FlagView;
import com.uiu.examintegrity.proctoring.application.view.QueueRow;
import com.uiu.examintegrity.proctoring.persistence.Flag;
import com.uiu.examintegrity.proctoring.persistence.FlagRepository;
import com.uiu.examintegrity.proctoring.persistence.FlagStatus;
import com.uiu.examintegrity.proctoring.persistence.RiskScore;
import com.uiu.examintegrity.proctoring.persistence.RiskScoreRepository;
import com.uiu.examintegrity.user.UserRepository;

@Service
public class FlagReviewService {
    private final FlagRepository flagRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final UserRepository userRepository;
    private final CourseAccess courseAccess;
    private final Clock clock;

    public FlagReviewService(FlagRepository flagRepository, RiskScoreRepository riskScoreRepository,
            UserRepository userRepository, CourseAccess courseAccess, Clock clock) {
        this.flagRepository = flagRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.userRepository = userRepository;
        this.courseAccess = courseAccess;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<QueueRow> queue(Long staffId) {
        List<Long> courses = courseAccess.staffedCourseIds(staffId);
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Flag> flags = flagRepository.findQueue(MonitorService.OPEN, courses);

        Map<Long, RiskScore> risks = new HashMap<>();
        riskScoreRepository.findAllById(flags.stream().map(f -> f.getSession().getId()).toList())
                .forEach(risk -> risks.put(risk.getSessionId(), risk));

        return flags.stream().map(flag -> {
            RiskScore risk = risks.get(flag.getSession().getId());
            return new QueueRow(flag.getId(), flag.getSession().getId(), flag.getSession().getExam().getId(),
                    flag.getSession().getExam().getTitle(), flag.getSession().getUser().getFullName(),
                    flag.getSession().getUser().getStudentId(), flag.getReason(), flag.getStatus().name(),
                    flag.getConfidence(), risk == null ? null : risk.getScore(),
                    risk == null ? null : risk.getTier().name(), signalNames(risk), flag.getCreatedAt());
        }).toList();
    }

    @Transactional
    public FlagView decide(Long sessionId, Long staffId, ReviewDecision decision, String note) {
        Flag flag = flagRepository.findFirstBySessionIdAndStatusIn(sessionId, MonitorService.OPEN)
                .orElseThrow(() -> new NotFoundException("This session has no open flag"));
        courseAccess.requireStaff(flag.getSession().getExam().getCourse().getId(), staffId);

        FlagStatus next = switch (decision) {
            case UNDER_REVIEW -> {
                if (flag.getStatus() != FlagStatus.RAISED) {
                    throw new ConflictException("This flag is already under review");
                }
                yield FlagStatus.UNDER_REVIEW;
            }
            case UPHOLD -> {
                if (note == null || note.isBlank()) {
                    throw new BadRequestException("Upholding a flag needs a note explaining why");
                }
                yield FlagStatus.UPHELD;
            }
            case DISMISS -> FlagStatus.DISMISSED;
        };

        flag.setStatus(next);
        flag.setReviewedBy(userRepository.getReferenceById(staffId));
        flag.setReviewedAt(clock.instant());
        if (note != null && !note.isBlank()) {
            flag.setReviewerNote(note.strip());
        }
        return FlagView.of(flagRepository.saveAndFlush(flag));
    }

    private static List<String> signalNames(RiskScore risk) {
        List<String> names = new ArrayList<>();
        if (risk == null) {
            return names;
        }
        for (Map<String, Object> row : risk.getBreakdown()) {
            Object name = row.containsKey("detector") ? row.get("detector") : row.get("signal");
            if (name != null && !names.contains(String.valueOf(name))) {
                names.add(String.valueOf(name));
            }
        }
        return names;
    }
}
