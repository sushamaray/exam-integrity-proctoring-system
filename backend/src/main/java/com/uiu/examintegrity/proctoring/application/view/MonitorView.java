package com.uiu.examintegrity.proctoring.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MonitorView(
        Long examId,
        String title,
        String courseCode,
        String status,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        List<Tile> sessions) {
    public record Tile(
            Long sessionId,
            String student,
            String studentId,
            String state,
            Instant startedAt,
            Instant expiresAt,
            BigDecimal score,
            String tier,
            BigDecimal cohortPercentile,
            String lastSignal,
            Instant lastSignalAt,
            String flagStatus) {
    }
}
