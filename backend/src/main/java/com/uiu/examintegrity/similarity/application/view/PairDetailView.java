package com.uiu.examintegrity.similarity.application.view;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PairDetailView(
        Long pairId,
        Long examId,
        String examTitle,
        String method,
        double score,
        String detail,
        Long questionId,
        String questionText,
        Side a,
        Side b,
        List<Map<String, Object>> spans,
        List<SharedMistake> sharedMistakes) {
    public record Side(Long sessionId, String student, String studentId, Instant submittedAt, String text) {
    }

    public record SharedMistake(Long questionId, String question, String option, String optionText,
            String correctOption, String correctText) {
    }
}
