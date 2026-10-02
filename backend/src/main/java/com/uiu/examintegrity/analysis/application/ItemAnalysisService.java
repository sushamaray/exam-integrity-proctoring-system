package com.uiu.examintegrity.analysis.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.analysis.application.view.ItemAnalysisView;
import com.uiu.examintegrity.analysis.persistence.ItemStats;
import com.uiu.examintegrity.analysis.persistence.ItemStatsRepository;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.course.CourseAccess;
import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;
import com.uiu.examintegrity.question.QuestionType;

/**
 * How well each question worked, once enough people have sat the exam.
 *
 * <p>Both numbers are computed from <strong>marks earned</strong> rather than right-or-wrong. For a
 * multiple-choice question the two definitions coincide, so nothing changes there; the gain is that
 * a coding question graded on how many tests it passed gets a difficulty and a discrimination too,
 * instead of being invisible.
 *
 * <p>This matters to more than question quality. {@code WrongAnswerAgreement} treats two students
 * picking the same wrong option as a collusion signal -- but an item with one dominant distractor
 * makes that coincidence common and innocent. A badly discriminating question is therefore noise in
 * the very data the collusion detector reads.
 */
@Service
public class ItemAnalysisService {
    /** A voided sitting must not shape the statistics, and an open one has nothing to say yet. */
    private static final Set<SessionStatus> COUNTED =
            EnumSet.of(SessionStatus.SUBMITTED, SessionStatus.AUTO_SUBMITTED);

    /**
     * The slice taken from each end when comparing strong against weak. 27% is the conventional
     * choice -- wide enough to be stable, narrow enough that the groups are genuinely different.
     */
    private static final double EXTREME_GROUP_FRACTION = 0.27;

    private static final int QUESTION_PREVIEW = 120;

    private final ItemStatsRepository itemStatsRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamSessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionOptionRepository optionRepository;
    private final CourseAccess courseAccess;
    private final Clock clock;

    public ItemAnalysisService(ItemStatsRepository itemStatsRepository, ExamRepository examRepository,
            ExamQuestionRepository examQuestionRepository, ExamSessionRepository sessionRepository,
            AnswerRepository answerRepository, QuestionOptionRepository optionRepository,
            CourseAccess courseAccess, Clock clock) {
        this.itemStatsRepository = itemStatsRepository;
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.optionRepository = optionRepository;
        this.courseAccess = courseAccess;
        this.clock = clock;
    }

    @Transactional
    public ItemAnalysisView recompute(Long examId, Long staffUserId) {
        Exam exam = staffedExam(examId, staffUserId);
        itemStatsRepository.deleteByExamId(examId);
        itemStatsRepository.saveAll(compute(exam));
        return view(examId, staffUserId);
    }

    /** Called by the demo seeder, which has no signed-in member of staff to check. */
    @Transactional
    public void recomputeUnchecked(Long examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new NotFoundException("No such exam"));
        itemStatsRepository.deleteByExamId(examId);
        itemStatsRepository.saveAll(compute(exam));
    }

    @Transactional(readOnly = true)
    public ItemAnalysisView view(Long examId, Long staffUserId) {
        Exam exam = staffedExam(examId, staffUserId);

        Map<Long, ExamQuestion> pool = new HashMap<>();
        examQuestionRepository.findPool(examId).forEach(eq -> pool.put(eq.getQuestion().getId(), eq));
        List<Long> order = pool.keySet().stream().sorted().toList();

        Map<Long, QuestionOption> correctByQuestion = new HashMap<>();
        for (QuestionOption option : optionRepository.findByQuestionIdInOrderByLabel(order)) {
            if (option.isCorrect()) {
                correctByQuestion.put(option.getQuestion().getId(), option);
            }
        }

        List<ItemStats> stats = itemStatsRepository.findByExamId(examId);
        stats.sort(Comparator.comparing(item -> order.indexOf(item.getQuestion().getId())));

        List<ItemAnalysisView.Item> items = new ArrayList<>();
        for (ItemStats item : stats) {
            Question question = item.getQuestion();
            ExamQuestion eq = pool.get(question.getId());
            if (eq == null) {
                continue;
            }
            items.add(new ItemAnalysisView.Item(
                    question.getId(),
                    order.indexOf(question.getId()) + 1,
                    question.getType().name(),
                    preview(question.getBody()),
                    item.getAttempts(),
                    item.getDifficultyIndex(),
                    item.getDiscriminationIndex(),
                    item.getAvgSeconds(),
                    shares(item, correctByQuestion.get(question.getId()))));
        }

        return new ItemAnalysisView(examId, exam.getTitle(),
                stats.stream().map(ItemStats::getComputedAt).filter(java.util.Objects::nonNull)
                        .max(Comparator.naturalOrder()).orElse(null),
                countedSittings(examId).size(), items);
    }

    private List<ItemAnalysisView.OptionShare> shares(ItemStats item, QuestionOption correct) {
        if (item.getOptionDistribution() == null) {
            return List.of();
        }
        String correctLabel = correct == null ? null : correct.getLabel();
        return item.getOptionDistribution().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ItemAnalysisView.OptionShare(
                        entry.getKey(), entry.getValue(), entry.getKey().equals(correctLabel)))
                .toList();
    }

    private Exam staffedExam(Long examId, Long staffUserId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new NotFoundException("No such exam"));
        courseAccess.requireStaff(exam.getCourse().getId(), staffUserId);
        return exam;
    }

    private List<ExamSessionEntity> countedSittings(Long examId) {
        return sessionRepository.findByExamId(examId).stream()
                .filter(sitting -> COUNTED.contains(sitting.getStatus()))
                .filter(sitting -> sitting.getTotalMarks() != null)
                .toList();
    }

    private List<ItemStats> compute(Exam exam) {
        List<ExamSessionEntity> sittings = countedSittings(exam.getId());
        if (sittings.isEmpty()) {
            return List.of();
        }

        List<ExamQuestion> pool = examQuestionRepository.findPool(exam.getId());
        Map<Long, BigDecimal> maxMarks = new HashMap<>();
        for (ExamQuestion eq : pool) {
            maxMarks.put(eq.getQuestion().getId(), marksFor(eq));
        }

        // How much of each question's marks each sitting earned. Only graded answers appear: an
        // ungraded written answer is absent, not a zero, or every essay would look impossible.
        Map<Long, Map<Long, BigDecimal>> earnedBySession = new HashMap<>();
        Map<Long, List<AnswerEntity>> answersBySession = new HashMap<>();
        for (ExamSessionEntity sitting : sittings) {
            List<AnswerEntity> answers = answerRepository.findWithOptionBySessionId(sitting.getId());
            answersBySession.put(sitting.getId(), answers);

            Map<Long, BigDecimal> earned = new HashMap<>();
            for (AnswerEntity answer : answers) {
                Long questionId = answer.getQuestion().getId();
                BigDecimal available = maxMarks.get(questionId);
                if (answer.getMarksAwarded() == null || available == null
                        || available.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                earned.put(questionId, answer.getMarksAwarded().divide(available, 6, RoundingMode.HALF_UP));
            }
            earnedBySession.put(sitting.getId(), earned);
        }

        List<ExamSessionEntity> ranked = new ArrayList<>(sittings);
        ranked.sort(Comparator.comparing(ExamSessionEntity::getTotalMarks).reversed());
        int groupSize = Math.max(1, (int) Math.round(ranked.size() * EXTREME_GROUP_FRACTION));
        List<ExamSessionEntity> upper = ranked.subList(0, groupSize);
        List<ExamSessionEntity> lower = ranked.subList(ranked.size() - groupSize, ranked.size());

        List<ItemStats> stats = new ArrayList<>();
        for (ExamQuestion eq : pool) {
            Question question = eq.getQuestion();
            ItemStats item = statsFor(exam, question, maxMarks.get(question.getId()),
                    sittings, earnedBySession, answersBySession, upper, lower);
            if (item != null) {
                stats.add(item);
            }
        }
        return stats;
    }

    private ItemStats statsFor(Exam exam, Question question, BigDecimal available,
            List<ExamSessionEntity> sittings, Map<Long, Map<Long, BigDecimal>> earnedBySession,
            Map<Long, List<AnswerEntity>> answersBySession,
            List<ExamSessionEntity> upper, List<ExamSessionEntity> lower) {
        Long questionId = question.getId();

        int attempts = 0;
        int fullMarks = 0;
        BigDecimal earnedTotal = BigDecimal.ZERO;
        long totalSeconds = 0;
        Map<String, Integer> chosen = new LinkedHashMap<>();

        for (ExamSessionEntity sitting : sittings) {
            BigDecimal earned = earnedBySession.get(sitting.getId()).get(questionId);
            if (earned == null) {
                continue;
            }
            attempts++;
            earnedTotal = earnedTotal.add(earned);
            if (earned.compareTo(BigDecimal.ONE) >= 0) {
                fullMarks++;
            }
            for (AnswerEntity answer : answersBySession.get(sitting.getId())) {
                if (!answer.getQuestion().getId().equals(questionId)) {
                    continue;
                }
                if (answer.getFirstSeenAt() != null && answer.getAnsweredAt() != null) {
                    totalSeconds += Duration.between(answer.getFirstSeenAt(), answer.getAnsweredAt()).toSeconds();
                }
                // Null for anything that is not multiple choice, which is why this is guarded.
                if (answer.getOption() != null) {
                    chosen.merge(answer.getOption().getLabel(), 1, Integer::sum);
                }
            }
        }

        // Nobody was shown it, or nobody who was has been graded yet. Either way there is nothing
        // honest to report, so the question simply does not appear.
        if (attempts == 0) {
            return null;
        }

        ItemStats item = new ItemStats(exam, question, attempts, fullMarks);
        item.setComputedAt(clock.instant());
        item.setDifficultyIndex(scaled(earnedTotal.divide(BigDecimal.valueOf(attempts), 6, RoundingMode.HALF_UP)));
        item.setDiscriminationIndex(discrimination(questionId, earnedBySession, upper, lower));
        item.setAvgSeconds(BigDecimal.valueOf(totalSeconds)
                .divide(BigDecimal.valueOf(attempts), 2, RoundingMode.HALF_UP));

        if (question.getType() == QuestionType.MCQ && !chosen.isEmpty()) {
            BigDecimal answered = BigDecimal.valueOf(attempts);
            Map<String, BigDecimal> distribution = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : chosen.entrySet()) {
                distribution.put(entry.getKey(), scaled(BigDecimal.valueOf(entry.getValue())
                        .divide(answered, 6, RoundingMode.HALF_UP)));
            }
            item.setOptionDistribution(distribution);
        }
        return item;
    }

    /**
     * The gap between how the strongest and the weakest did on this one question.
     *
     * <p>Each side averages over <em>those in that group who actually answered it</em>, not over the
     * whole group. With question pooling a top-third student may never have been shown the item, and
     * dividing by the group size would silently count their absence as a wrong answer.
     */
    private BigDecimal discrimination(Long questionId, Map<Long, Map<Long, BigDecimal>> earnedBySession,
            List<ExamSessionEntity> upper, List<ExamSessionEntity> lower) {
        BigDecimal top = groupMean(questionId, earnedBySession, upper);
        BigDecimal bottom = groupMean(questionId, earnedBySession, lower);
        if (top == null || bottom == null) {
            return null;
        }
        return scaled(top.subtract(bottom));
    }

    private BigDecimal groupMean(Long questionId, Map<Long, Map<Long, BigDecimal>> earnedBySession,
            List<ExamSessionEntity> group) {
        BigDecimal total = BigDecimal.ZERO;
        int seen = 0;
        for (ExamSessionEntity sitting : group) {
            BigDecimal earned = earnedBySession.get(sitting.getId()).get(questionId);
            if (earned != null) {
                total = total.add(earned);
                seen++;
            }
        }
        return seen == 0 ? null : total.divide(BigDecimal.valueOf(seen), 6, RoundingMode.HALF_UP);
    }

    /** The columns are {@code numeric(5,4)}, so everything stored has to fit that. */
    private static BigDecimal scaled(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal marksFor(ExamQuestion eq) {
        return eq.getMarks() != null ? eq.getMarks() : eq.getQuestion().getMaxMarks();
    }

    private static String preview(String body) {
        if (body == null) {
            return "";
        }
        String flat = body.strip().replaceAll("\\s+", " ");
        return flat.length() <= QUESTION_PREVIEW ? flat : flat.substring(0, QUESTION_PREVIEW) + "…";
    }
}
