package com.uiu.examintegrity.exam.application.view;

import java.time.Instant;

public record OpenExamView(
        Long id,
        String title,
        String courseCode,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        Long sessionId,
        String sessionState) {
}
