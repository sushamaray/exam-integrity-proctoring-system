package com.uiu.examintegrity.exam.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.uiu.examintegrity.exam.domain.AnswerValue;
import com.uiu.examintegrity.exam.domain.grading.GradableQuestion;
import com.uiu.examintegrity.exam.domain.grading.GradableQuestion.CodingOutcome;
import com.uiu.examintegrity.exam.domain.grading.GradeResult;
import com.uiu.examintegrity.exam.domain.grading.GradingStrategy;
import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.CodeSubmission;
import com.uiu.examintegrity.exam.persistence.CodeSubmissionRepository;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.GradingStatus;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;
import com.uiu.examintegrity.question.QuestionType;

@Service
public class GradingService {
    private final List<GradingStrategy> strategies;
    private final AnswerRepository answerRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final CodeSubmissionRepository codeSubmissionRepository;

    public GradingService(List<GradingStrategy> strategies, AnswerRepository answerRepository,
            ExamQuestionRepository examQuestionRepository, QuestionOptionRepository questionOptionRepository,
            CodeSubmissionRepository codeSubmissionRepository) {
        this.strategies = strategies;
        this.answerRepository = answerRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.codeSubmissionRepository = codeSubmissionRepository;
    }

    public void grade(ExamSessionEntity session, Instant now) {
        Map<Long, GradableQuestion> questions = gradableQuestions(session);

        BigDecimal total = BigDecimal.ZERO;
        boolean anyPending = false;

        for (AnswerEntity answer : answerRepository.findWithOptionBySessionId(session.getId())) {
            AnswerValue value = valueOf(answer);
            GradableQuestion question = questions.get(answer.getQuestion().getId());
            if (value == null || question == null) {
                continue;
            }

            GradeResult result = strategyFor(question.type()).grade(value, question);
            if (result.pending()) {
                anyPending = true;
                answer.setMarksAwarded(null);
                answer.setGradedAt(null);
            } else {
                answer.setMarksAwarded(result.marks());
                answer.setGradedAt(now);
                total = total.add(result.marks());
            }
        }

        session.setTotalMarks(total);
        session.setGradingStatus(anyPending ? GradingStatus.PARTIAL : GradingStatus.COMPLETE);
    }

    GradingStrategy strategyFor(QuestionType type) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No grading strategy for " + type));
    }

    private Map<Long, GradableQuestion> gradableQuestions(ExamSessionEntity session) {
        List<ExamQuestion> pool = examQuestionRepository.findPool(session.getExam().getId());
        Map<Long, CodingOutcome> coding = codingOutcomes(session.getId());

        Map<Long, String> correctLabels = new HashMap<>();
        List<Long> ids = pool.stream().map(eq -> eq.getQuestion().getId()).toList();
        for (QuestionOption option : questionOptionRepository.findByQuestionIdInOrderByLabel(ids)) {
            if (option.isCorrect()) {
                correctLabels.put(option.getQuestion().getId(), option.getLabel());
            }
        }

        Map<Long, GradableQuestion> byId = new HashMap<>();
        for (ExamQuestion eq : pool) {
            Long id = eq.getQuestion().getId();

            BigDecimal marks = eq.getMarks() != null ? eq.getMarks() : eq.getQuestion().getMaxMarks();
            byId.put(id, new GradableQuestion(id, eq.getQuestion().getType(), correctLabels.get(id), marks,
                    coding.get(id)));
        }
        return byId;
    }

    /**
     * The newest final submission per coding question. One query, newest first, keeping the
     * first hit -- a student who submits a question twice is graded on the later attempt.
     */
    private Map<Long, CodingOutcome> codingOutcomes(Long sessionId) {
        Map<Long, CodingOutcome> byQuestion = new HashMap<>();
        for (CodeSubmission submission : codeSubmissionRepository.findFinalBySessionId(sessionId)) {
            if (submission.getTotalCount() == null || submission.getPassedCount() == null) {
                continue;
            }
            byQuestion.putIfAbsent(submission.getQuestion().getId(),
                    new CodingOutcome(submission.getPassedCount(), submission.getTotalCount()));
        }
        return byQuestion;
    }

    private static AnswerValue valueOf(AnswerEntity answer) {
        if (answer.getOption() != null) {
            return AnswerValue.choice(answer.getOption().getLabel());
        }
        return answer.getTextAnswer() != null ? AnswerValue.text(answer.getTextAnswer()) : null;
    }
}
