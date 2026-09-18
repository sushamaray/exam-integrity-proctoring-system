package com.uiu.examintegrity.proctoring.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QueueRow(
        Long flagId,
        Long sessionId,
        Long examId,
        String examTitle,
        String student,
        String studentId,
        String reason,
        String status,
        BigDecimal confidence,
        BigDecimal score,
        String tier,
        List<String> signals,
        Instant createdAt) {
}
