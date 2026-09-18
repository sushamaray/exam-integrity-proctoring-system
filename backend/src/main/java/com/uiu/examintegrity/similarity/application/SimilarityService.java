package com.uiu.examintegrity.similarity.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.course.CourseAccess;
import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.similarity.application.view.GraphView;
import com.uiu.examintegrity.similarity.application.view.PairDetailView;
import com.uiu.examintegrity.similarity.domain.PairResult;
import com.uiu.examintegrity.similarity.domain.SimilarityReport;
import com.uiu.examintegrity.similarity.domain.SimilarityStrategy;
import com.uiu.examintegrity.similarity.domain.Span;
import com.uiu.examintegrity.similarity.domain.UnionFind;
import com.uiu.examintegrity.similarity.persistence.SimilarityPair;
import com.uiu.examintegrity.similarity.persistence.SimilarityPairRepository;

@Service
public class SimilarityService {
    private static final Set<SessionStatus> COMPARED = EnumSet.of(SessionStatus.SUBMITTED, SessionStatus.AUTO_SUBMITTED);
    private static final int QUESTION_PREVIEW = 90;

    private final ExamSimilarityPipeline pipeline;
    private final List<SimilarityStrategy> strategies;
    private final SimilarityPairRepository pairRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final CourseAccess courseAccess;

    public SimilarityService(ExamSimilarityPipeline pipeline, List<SimilarityStrategy> strategies,
            SimilarityPairRepository pairRepository, ExamRepository examRepository,
            ExamSessionRepository sessionRepository, AnswerRepository answerRepository,
            QuestionRepository questionRepository, QuestionOptionRepository optionRepository,
            CourseAccess courseAccess) {
        this.pipeline = pipeline;
        this.strategies = strategies;
        this.pairRepository = pairRepository;
        this.examRepository = examRepository;
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.courseAccess = courseAccess;
    }

    @Transactional
    public GraphView run(Long examId, Long staffId) {
        requireStaff(examId, staffId);
        runFor(examId);
        return graphFor(examId);
    }

    @Transactional
    public SimilarityReport runFor(Long examId) {
        if (sessionRepository.existsByExamIdAndStatus(examId, SessionStatus.IN_PROGRESS)) {
            throw new ConflictException("Collusion detection runs once every sitting has been submitted");
        }
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Not found"));

        SimilarityReport report = pipeline.run(examId);

        pairRepository.deleteByExamId(examId);
        List<SimilarityPair> rows = new ArrayList<>();
        for (PairResult edge : report.edges()) {
            SimilarityPair row = new SimilarityPair(exam,
                    sessionRepository.getReferenceById(edge.pair().a()),
                    sessionRepository.getReferenceById(edge.pair().b()),
                    BigDecimal.valueOf(edge.score()).setScale(4, RoundingMode.HALF_UP),
                    edge.method());
            if (edge.questionId() != null) {
                row.setQuestion(questionRepository.getReferenceById(edge.questionId()));
            }
            row.setMatchedSpans(evidence(edge));
            rows.add(row);
        }
        pairRepository.saveAll(rows);
        return report;
    }

    private static List<Map<String, Object>> evidence(PairResult edge) {
        List<Map<String, Object>> evidence = new ArrayList<>();
        for (Span span : edge.spans()) {
            evidence.add(Map.of("aStart", span.aStart(), "aEnd", span.aEnd(),
                    "bStart", span.bStart(), "bEnd", span.bEnd()));
        }
        for (Long questionId : edge.matchedQuestionIds()) {
            evidence.add(Map.of("questionId", questionId));
        }
        return evidence;
    }

    @Transactional(readOnly = true)
    public GraphView graph(Long examId, Long staffId) {
        requireStaff(examId, staffId);
        return graphFor(examId);
    }

    private GraphView graphFor(Long examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Not found"));
        List<SimilarityPair> pairs = pairRepository.findGraph(examId);

        UnionFind components = new UnionFind();
        pairs.forEach(p -> components.union(p.getSessionA().getId(), p.getSessionB().getId()));

        Map<Long, Integer> numbers = new HashMap<>();
        List<GraphView.Node> nodes = new ArrayList<>();
        for (ExamSessionEntity session : sessionRepository.findWithUserByExamId(examId)) {
            if (!COMPARED.contains(session.getStatus())) {
                continue;
            }
            Integer cluster = null;
            if (components.sizeOf(session.getId()) > 1) {
                cluster = numbers.computeIfAbsent(components.find(session.getId()), root -> numbers.size() + 1);
            }
            nodes.add(new GraphView.Node(session.getId(), session.getUser().getFullName(),
                    session.getUser().getStudentId(), cluster));
        }

        List<GraphView.Edge> edges = pairs.stream().map(p -> new GraphView.Edge(p.getId(),
                p.getSessionA().getId(), p.getSessionB().getId(), p.getScore().doubleValue(), p.getMethod(),
                p.getQuestion() == null ? null : p.getQuestion().getId(),
                p.getQuestion() == null ? null : preview(p.getQuestion().getBody()),
                summarise(p))).toList();

        Instant computedAt = pairs.stream().map(SimilarityPair::getComputedAt)
                .filter(Objects::nonNull).max(Instant::compareTo).orElse(null);
        Map<String, Double> thresholds = new LinkedHashMap<>();
        strategies.forEach(s -> thresholds.put(s.method(), s.threshold()));

        return new GraphView(examId, exam.getTitle(), exam.getCourse().getCode(), computedAt, thresholds,
                nodes, edges);
    }

    private static String summarise(SimilarityPair pair) {
        List<Map<String, Object>> evidence = pair.getMatchedSpans() == null ? List.of() : pair.getMatchedSpans();
        long questions = evidence.stream().filter(e -> e.containsKey("questionId")).count();
        long spans = evidence.size() - questions;
        return questions > 0
                ? questions + " matching wrong answer" + (questions == 1 ? "" : "s")
                : spans + " shared phrase" + (spans == 1 ? "" : "s");
    }

    @Transactional(readOnly = true)
    public PairDetailView pair(Long pairId, Long staffId) {
        SimilarityPair pair = pairRepository.findDetail(pairId).orElseThrow(() -> new NotFoundException("Not found"));
        courseAccess.requireStaff(pair.getExam().getCourse().getId(), staffId);

        ExamSessionEntity a = pair.getSessionA();
        ExamSessionEntity b = pair.getSessionB();
        Question question = pair.getQuestion();
        List<Map<String, Object>> evidence = pair.getMatchedSpans() == null ? List.of() : pair.getMatchedSpans();

        Map<Long, Map<Long, AnswerEntity>> answers = new HashMap<>();
        for (AnswerEntity answer : answerRepository.findForSessions(List.of(a.getId(), b.getId()))) {
            answers.computeIfAbsent(answer.getSession().getId(), id -> new HashMap<>())
                    .put(answer.getQuestion().getId(), answer);
        }
        String textA = question == null ? null : textOf(answers, a.getId(), question.getId());
        String textB = question == null ? null : textOf(answers, b.getId(), question.getId());

        List<Map<String, Object>> spans = evidence.stream().filter(e -> e.containsKey("aStart")).toList();
        List<PairDetailView.SharedMistake> mistakes = evidence.stream()
                .filter(e -> e.containsKey("questionId"))
                .map(e -> ((Number) e.get("questionId")).longValue())
                .map(q -> sharedMistake(q, answers.getOrDefault(a.getId(), Map.of()).get(q)))
                .filter(Objects::nonNull)
                .toList();

        String detail = summarise(pair);
        return new PairDetailView(pair.getId(), pair.getExam().getId(), pair.getExam().getTitle(), pair.getMethod(),
                pair.getScore().doubleValue(), detail,
                question == null ? null : question.getId(), question == null ? null : question.getBody(),
                side(a, textA), side(b, textB), spans, mistakes);
    }

    private PairDetailView.SharedMistake sharedMistake(Long questionId, AnswerEntity chosen) {
        if (chosen == null || chosen.getOption() == null) {
            return null;
        }
        QuestionOption correct = optionRepository.findByQuestionIdOrderByLabel(questionId).stream()
                .filter(QuestionOption::isCorrect).findFirst().orElse(null);
        return new PairDetailView.SharedMistake(questionId, chosen.getQuestion().getBody(),
                chosen.getOption().getLabel(), chosen.getOption().getBody(),
                correct == null ? null : correct.getLabel(), correct == null ? null : correct.getBody());
    }

    private static String textOf(Map<Long, Map<Long, AnswerEntity>> answers, Long sessionId, Long questionId) {
        AnswerEntity answer = answers.getOrDefault(sessionId, Map.of()).get(questionId);
        return answer == null ? null : answer.getTextAnswer();
    }

    private static PairDetailView.Side side(ExamSessionEntity session, String text) {
        return new PairDetailView.Side(session.getId(), session.getUser().getFullName(),
                session.getUser().getStudentId(), session.getSubmittedAt(), text);
    }

    private static String preview(String body) {
        return body.length() <= QUESTION_PREVIEW ? body : body.substring(0, QUESTION_PREVIEW - 1) + "…";
    }

    private void requireStaff(Long examId, Long staffId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new NotFoundException("Not found"));
        courseAccess.requireStaff(exam.getCourse().getId(), staffId);
    }
}
