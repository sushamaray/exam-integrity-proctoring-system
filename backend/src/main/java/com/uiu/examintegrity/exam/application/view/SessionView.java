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
    /**
     * @param starterCode the template a coding question opens with, null for other types
     * @param tests       the sample cases only -- a hidden case never reaches the browser,
     *                    because its expected output is the answer
     */
    public record QuestionView(
            Long id,
            String type,
            String topic,
            String bloom,
            String text,
            List<OptionView> options,
            String starterCode,
            List<TestCaseView> tests) {
    }

    public record OptionView(String id, String text) {
    }

    public record TestCaseView(String name, String input, String expected) {
    }
}
