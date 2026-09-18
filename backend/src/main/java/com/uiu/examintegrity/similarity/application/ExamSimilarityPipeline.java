package com.uiu.examintegrity.similarity.application;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.similarity.domain.SessionAnswers;
import com.uiu.examintegrity.similarity.domain.SimilarityStrategy;
import com.uiu.examintegrity.similarity.domain.StrategyPipeline;

@Component
public class ExamSimilarityPipeline extends StrategyPipeline {
    private static final Set<SessionStatus> COMPARED = EnumSet.of(SessionStatus.SUBMITTED, SessionStatus.AUTO_SUBMITTED);

    private final ExamSessionRepository sessionRepository;
    private final AnswerRepository answerRepository;

    public ExamSimilarityPipeline(List<SimilarityStrategy> strategies, ExamSessionRepository sessionRepository,
            AnswerRepository answerRepository) {
        super(strategies);
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
    }

    @Override
    protected List<SessionAnswers> load(Long examId) {
        Map<Long, Map<Long, SessionAnswers.Answer>> bySession = new HashMap<>();
        for (AnswerEntity answer : answerRepository.findForExam(examId)) {
            SessionAnswers.Answer value = new SessionAnswers.Answer(
                    answer.getQuestion().getId(),
                    answer.getQuestion().getType(),
                    answer.getOption() == null ? null : answer.getOption().getLabel(),
                    answer.getOption() == null ? null : answer.getOption().isCorrect(),
                    answer.getTextAnswer(),
                    null);
            bySession.computeIfAbsent(answer.getSession().getId(), id -> new HashMap<>())
                    .put(value.questionId(), value);
        }

        List<SessionAnswers> sittings = new ArrayList<>();
        for (ExamSessionEntity session : sessionRepository.findWithUserByExamId(examId)) {
            if (COMPARED.contains(session.getStatus())) {
                sittings.add(new SessionAnswers(session.getId(), session.getUser().getFullName(),
                        bySession.getOrDefault(session.getId(), Map.of())));
            }
        }
        return sittings;
    }
}
