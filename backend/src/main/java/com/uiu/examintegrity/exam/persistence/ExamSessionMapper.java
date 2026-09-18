package com.uiu.examintegrity.exam.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.uiu.examintegrity.exam.domain.ExamSession;
import com.uiu.examintegrity.exam.domain.Paper;
import com.uiu.examintegrity.exam.domain.SessionState;

public final class ExamSessionMapper {
    private ExamSessionMapper() {
    }

    public static ExamSession toDomain(ExamSessionEntity entity) {
        return ExamSession.restore(
                entity.getId(),
                entity.getExam().getId(),
                entity.getUser().getId(),
                SessionState.of(entity.getStatus().name()),
                entity.getStartedAt(),
                entity.getExpiresAt(),
                entity.getSubmittedAt(),
                paperOf(entity));
    }

    public static void apply(ExamSession session, ExamSessionEntity entity) {
        entity.setStatus(SessionStatus.valueOf(session.state().name()));
        entity.setStartedAt(session.startedAt());
        entity.setExpiresAt(session.expiresAt());
        entity.setSubmittedAt(session.submittedAt());

        Paper paper = session.paper();
        if (paper != null) {
            entity.setQuestionOrder(paper.questionOrder());

            Map<String, List<String>> options = new LinkedHashMap<>();
            paper.optionOrder().forEach((id, labels) -> options.put(String.valueOf(id), labels));
            entity.setOptionOrder(options);
        }
    }

    private static Paper paperOf(ExamSessionEntity entity) {
        if (entity.getQuestionOrder() == null) {
            return null;
        }
        Map<Long, List<String>> options = new LinkedHashMap<>();
        if (entity.getOptionOrder() != null) {
            entity.getOptionOrder().forEach((id, labels) -> options.put(Long.valueOf(id), labels));
        }
        return new Paper(entity.getQuestionOrder(), options);
    }
}
