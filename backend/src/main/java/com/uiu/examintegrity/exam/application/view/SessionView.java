package com.uiu.examintegrity.exam.application.view;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SessionView(
        Long id,
        Long examId,
        String title,
        String state,
        long remainingSeconds,
        Instant expiresAt,
        List<QuestionView> questions,
        Map<Long, String> answers) {
    public record QuestionView(
            Long id,
            String type,
            String topic,
            String bloom,
            String text,
            List<OptionView> options) {
    }

    public record OptionView(String id, String text) {
    }
}
