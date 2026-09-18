package com.uiu.examintegrity.exam.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.uiu.examintegrity.exam.application.view.SessionView.OptionView;

public record ReviewView(
        Long sessionId,
        String title,
        Instant submittedAt,
        BigDecimal totalMarks,
        BigDecimal maxMarks,
        List<ReviewedQuestion> questions) {
    public record ReviewedQuestion(
            Long id,
            String type,
            String topic,
            String bloom,
            String text,
            List<OptionView> options,
            String correct,
            String yourAnswer,
            String explanation,
            BigDecimal marksAwarded,
            BigDecimal maxMarks) {
    }
}
