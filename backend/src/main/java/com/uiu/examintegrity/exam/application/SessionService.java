package com.uiu.examintegrity.exam.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.course.CourseRole;
import com.uiu.examintegrity.course.Enrollment;
import com.uiu.examintegrity.course.EnrollmentRepository;
import com.uiu.examintegrity.exam.application.view.OpenExamView;
import com.uiu.examintegrity.exam.application.view.SavedAnswer;
import com.uiu.examintegrity.exam.application.view.SessionView;
import com.uiu.examintegrity.exam.application.view.SessionView.OptionView;
import com.uiu.examintegrity.exam.application.view.SessionView.QuestionView;
import com.uiu.examintegrity.exam.application.view.SubmitResult;
import com.uiu.examintegrity.exam.domain.AnswerValue;
import com.uiu.examintegrity.exam.domain.ExamSession;
import com.uiu.examintegrity.exam.domain.Paper;
import com.uiu.examintegrity.exam.domain.QuestionDraw;
import com.uiu.examintegrity.exam.domain.QuestionDraw.DrawRules;
import com.uiu.examintegrity.exam.domain.QuestionDraw.PoolItem;
import com.uiu.examintegrity.exam.domain.RecordedAnswer;
import com.uiu.examintegrity.exam.domain.SessionClosed;
import com.uiu.examintegrity.exam.domain.SessionStarted;
import com.uiu.examintegrity.exam.domain.draw.RandomDraw;
import com.uiu.examintegrity.exam.domain.draw.TopicBalancedDraw;
import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionMapper;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.ExamStatus;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.user.UserRepository;

@Service
public class SessionService {
    private static final Set<ExamStatus> SITTABLE = EnumSet.of(ExamStatus.SCHEDULED, ExamStatus.LIVE);

    private static final QuestionDraw WHOLE_POOL = new RandomDraw();
    private static final QuestionDraw SUBSET = new TopicBalancedDraw();

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamSessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final GradingService gradingService;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final TransactionTemplate transaction;

    private final RandomGenerator random = new SecureRandom();

    public SessionService(ExamRepository examRepository, ExamQuestionRepository examQuestionRepository,
            ExamSessionRepository sessionRepository, AnswerRepository answerRepository,
            QuestionRepository questionRepository, QuestionOptionRepository optionRepository,
            EnrollmentRepository enrollmentRepository, UserRepository userRepository,
            GradingService gradingService, ApplicationEventPublisher events, Clock clock,
            PlatformTransactionManager transactionManager) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.gradingService = gradingService;
        this.events = events;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<OpenExamView> listOpenExams(Long userId) {
        List<Long> courseIds = enrollmentRepository.findByUserIdAndRoleInCourse(userId, CourseRole.STUDENT)
                .stream().map(Enrollment::getCourse).map(course -> course.getId()).toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }

        List<Exam> exams = examRepository.findOpen(courseIds, SITTABLE, clock.instant());
        Map<Long, ExamSessionEntity> sittings = new HashMap<>();
        for (ExamSessionEntity sitting : sessionRepository.findByUserIdAndExamIdIn(
                userId, exams.stream().map(Exam::getId).toList())) {
            sittings.put(sitting.getExam().getId(), sitting);
        }

        return exams.stream().map(exam -> {
            ExamSessionEntity sitting = sittings.get(exam.getId());
            return new OpenExamView(exam.getId(), exam.getTitle(), exam.getCourse().getCode(),
                    exam.getStartsAt(), exam.getEndsAt(), exam.getDurationMinutes(),
                    sitting == null ? null : sitting.getId(),
                    sitting == null ? null : sitting.getStatus().name());
        }).toList();
    }

    public SessionView startOrResume(Long examId, Long userId, String ipAddress, String userAgent) {
        try {
            return transaction.execute(status -> startInTransaction(examId, userId, ipAddress, userAgent));
        } catch (DataIntegrityViolationException raced) {
            return transaction.execute(status -> {
                ExamSessionEntity winner = sessionRepository.findByExamIdAndUserId(examId, userId)
                        .orElseThrow(() -> raced);
                return resumeLocked(winner.getId(), userId);
            });
        }
    }

    private SessionView startInTransaction(Long examId, Long userId, String ipAddress, String userAgent) {
        var existing = sessionRepository.findByExamIdAndUserId(examId, userId);
        if (existing.isPresent()) {
            return resumeLocked(existing.get().getId(), userId);
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new NotFoundException("No such exam"));
        if (!enrollmentRepository.existsByCourseIdAndUserIdAndRoleInCourse(
                exam.getCourse().getId(), userId, CourseRole.STUDENT)) {
            throw new NotFoundException("No such exam");
        }

        Instant now = clock.instant();
        if (!SITTABLE.contains(exam.getStatus())
                || now.isBefore(exam.getStartsAt()) || !now.isBefore(exam.getEndsAt())) {
            throw new ConflictException("This exam is not open");
        }

        List<ExamQuestion> pool = examQuestionRepository.findPool(examId);
        if (pool.isEmpty()) {
            throw new ConflictException("This exam has no questions yet");
        }

        ExamSession session = ExamSession.allocate(examId, userId);
        session.start(now, Duration.ofMinutes(exam.getDurationMinutes()), exam.getEndsAt(),
                drawPaper(exam, pool));

        ExamSessionEntity entity = new ExamSessionEntity(exam, userRepository.getReferenceById(userId));
        ExamSessionMapper.apply(session, entity);

        entity.setConsentAt(now);
        entity.setIpAddress(ipAddress);
        entity.setUserAgent(userAgent);

        entity = sessionRepository.saveAndFlush(entity);
        events.publishEvent(new SessionStarted(entity.getId(), examId));

        return view(entity, ExamSessionMapper.toDomain(entity), now);
    }

    private Paper drawPaper(Exam exam, List<ExamQuestion> pool) {
        Map<Long, List<String>> labels = new HashMap<>();
        List<Long> ids = pool.stream().map(eq -> eq.getQuestion().getId()).toList();
        for (QuestionOption option : optionRepository.findByQuestionIdInOrderByLabel(ids)) {
            labels.computeIfAbsent(option.getQuestion().getId(), id -> new ArrayList<>()).add(option.getLabel());
        }

        List<PoolItem> items = pool.stream()
                .map(ExamQuestion::getQuestion)
                .map(q -> new PoolItem(q.getId(), q.getTopic(), labels.getOrDefault(q.getId(), List.of())))
                .toList();
        DrawRules rules = new DrawRules(exam.getQuestionsToDraw(),
                exam.isShuffleQuestions(), exam.isShuffleOptions());

        boolean subset = exam.getQuestionsToDraw() != null && exam.getQuestionsToDraw() < pool.size();
        return (subset ? SUBSET : WHOLE_POOL).draw(items, rules, random);
    }

    @Transactional
    public SessionView resume(Long sessionId, Long userId) {
        return resumeLocked(sessionId, userId);
    }

    private SessionView resumeLocked(Long sessionId, Long userId) {
        ExamSessionEntity entity = lockOwned(sessionId, userId);
        ExamSession session = ExamSessionMapper.toDomain(entity);
        Instant now = clock.instant();

        if (session.state().acceptsAnswers() && session.isExpired(now)) {
            close(entity, session, now, true);
        }
        return view(entity, session, now);
    }

    @Transactional
    public SavedAnswer saveAnswer(Long sessionId, Long userId, Long questionId, AnswerValue value) {
        ExamSessionEntity entity = lockOwned(sessionId, userId);
        ExamSession session = ExamSessionMapper.toDomain(entity);
        Instant now = clock.instant();

        RecordedAnswer recorded = session.recordAnswer(questionId, value, now);

        Long optionId = null;
        if (value.isChoice()) {
            optionId = optionRepository.findByQuestionIdAndLabel(questionId, value.optionLabel())
                    .orElseThrow(() -> new NotFoundException("No such option"))
                    .getId();
        }
        answerRepository.upsert(sessionId, questionId, optionId, value.text(), recorded.at());

        return new SavedAnswer(questionId, recorded.at(), session.remainingSeconds(now));
    }

    @Transactional
    public SubmitResult submit(Long sessionId, Long userId) {
        ExamSessionEntity entity = lockOwned(sessionId, userId);
        ExamSession session = ExamSessionMapper.toDomain(entity);
        Instant now = clock.instant();

        close(entity, session, now, session.isExpired(now));
        return new SubmitResult(sessionId, session.state().name(), session.submittedAt());
    }

    @Transactional
    public void autoSubmit(Long sessionId) {
        ExamSessionEntity entity = sessionRepository.findLockedById(sessionId)
                .orElseThrow(() -> new NotFoundException("No such session"));
        close(entity, ExamSessionMapper.toDomain(entity), clock.instant(), true);
    }

    private void close(ExamSessionEntity entity, ExamSession session, Instant now, boolean timedOut) {
        if (timedOut) {
            session.autoSubmit(now);
        } else {
            session.submit(now);
        }
        ExamSessionMapper.apply(session, entity);
        gradingService.grade(entity, now);
        events.publishEvent(new SessionClosed(entity.getId(), session.examId()));
    }

    private ExamSessionEntity lockOwned(Long sessionId, Long userId) {
        ExamSessionEntity entity = sessionRepository.findLockedById(sessionId)
                .orElseThrow(() -> new NotFoundException("No such session"));
        if (!entity.getUser().getId().equals(userId)) {
            throw new NotFoundException("No such session");
        }
        return entity;
    }

    private SessionView view(ExamSessionEntity entity, ExamSession session, Instant now) {
        Paper paper = session.paper();

        Map<Long, Question> questions = new HashMap<>();
        for (Question question : questionRepository.findAllById(paper.questionOrder())) {
            questions.put(question.getId(), question);
        }
        Map<Long, Map<String, String>> optionText = new HashMap<>();
        for (QuestionOption option : optionRepository.findByQuestionIdInOrderByLabel(paper.questionOrder())) {
            optionText.computeIfAbsent(option.getQuestion().getId(), id -> new HashMap<>())
                    .put(option.getLabel(), option.getBody());
        }

        List<QuestionView> questionViews = new ArrayList<>();
        for (Long id : paper.questionOrder()) {
            Question question = questions.get(id);
            Map<String, String> texts = optionText.getOrDefault(id, Map.of());
            List<OptionView> options = paper.optionsFor(id).stream()
                    .map(label -> new OptionView(label, texts.get(label)))
                    .toList();
            questionViews.add(new QuestionView(id, question.getType().name(), question.getTopic(),
                    question.getBloomLevel(), question.getBody(), options));
        }

        Map<Long, String> answers = new LinkedHashMap<>();
        for (AnswerEntity answer : answerRepository.findWithOptionBySessionId(entity.getId())) {
            String value = answer.getOption() != null ? answer.getOption().getLabel() : answer.getTextAnswer();
            if (value != null) {
                answers.put(answer.getQuestion().getId(), value);
            }
        }

        return new SessionView(entity.getId(), session.examId(), entity.getExam().getTitle(),
                session.state().name(), session.remainingSeconds(now), session.expiresAt(),
                questionViews, answers);
    }
}
