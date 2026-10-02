package com.uiu.examintegrity.exam.application.view;

import java.time.Instant;

/**
 * An exam a student may sit.
 *
 * @param coding true when the paper holds coding questions, so the dashboard links to the
 *               editor at /student/coding rather than the objective runner
 */
public record OpenExamView(
        Long id,
        String title,
        String courseCode,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        Long sessionId,
        String sessionState,
        boolean coding) {
}
