package com.uiu.examintegrity.exam.application.view;

import java.math.BigDecimal;
import java.time.Instant;

public record SittingSummary(
        Long sessionId,
        Long examId,
        String title,
        String courseCode,
        Instant submittedAt,
        String state,
        BigDecimal totalMarks,
        BigDecimal maxMarks,
        String gradingStatus) {
}
