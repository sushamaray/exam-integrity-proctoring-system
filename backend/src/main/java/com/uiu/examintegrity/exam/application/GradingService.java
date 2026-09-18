package com.uiu.examintegrity.exam.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.uiu.examintegrity.exam.domain.AnswerValue;
import com.uiu.examintegrity.exam.domain.grading.GradableQuestion;
import com.uiu.examintegrity.exam.domain.grading.GradeResult;
import com.uiu.examintegrity.exam.domain.grading.GradingStrategy;
import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
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

    public GradingService(List<GradingStrategy> strategies, AnswerRepository answerRepository,
            ExamQuestionRepository examQuestionRepository, QuestionOptionRepository questionOptionRepository) {
        this.strategies = strategies;
        this.answerRepository = answerRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionOptionRepository = questionOptionRepository;
    }

    public void grade(ExamSessionEntity session, Instant now) {
        Map<Long, GradableQuestion> questions = gradableQuestions(session.getExam().getId());

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

    private Map<Long, GradableQuestion> gradableQuestions(Long examId) {
        List<ExamQuestion> pool = examQuestionRepository.findPool(examId);

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
            byId.put(id, new GradableQuestion(id, eq.getQuestion().getType(), correctLabels.get(id), marks));
        }
        return byId;
    }

    private static AnswerValue valueOf(AnswerEntity answer) {
        if (answer.getOption() != null) {
            return AnswerValue.choice(answer.getOption().getLabel());
        }
        return answer.getTextAnswer() != null ? AnswerValue.text(answer.getTextAnswer()) : null;
    }
}
