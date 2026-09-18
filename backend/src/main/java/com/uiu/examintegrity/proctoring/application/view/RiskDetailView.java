package com.uiu.examintegrity.proctoring.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RiskDetailView(
        Long sessionId,
        Long examId,
        String examTitle,
        String student,
        String studentId,
        String state,
        Instant startedAt,
        Instant expiresAt,
        Instant submittedAt,
        BigDecimal score,
        String tier,
        BigDecimal cohortPercentile,
        Instant updatedAt,
        List<Map<String, Object>> breakdown,
        List<EventView> events,
        FlagView flag) {
    public record EventView(
            Long id,
            String type,
            Instant occurredAt,
            Instant receivedAt,
            Long questionId,
            Map<String, Object> payload,
            boolean hot) {
    }
}
