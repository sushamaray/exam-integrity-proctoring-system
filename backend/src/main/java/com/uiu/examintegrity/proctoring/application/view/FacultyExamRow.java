package com.uiu.examintegrity.proctoring.application.view;

import java.time.Instant;

public record FacultyExamRow(
        Long id,
        String title,
        String courseCode,
        String status,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        long poolSize,
        Integer questionsToDraw,
        long sittings,
        long inProgress,
        long openFlags) {
}
