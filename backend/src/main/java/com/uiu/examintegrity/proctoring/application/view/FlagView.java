package com.uiu.examintegrity.proctoring.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.uiu.examintegrity.proctoring.persistence.Flag;

public record FlagView(
        Long id,
        Long sessionId,
        String status,
        String reason,
        BigDecimal confidence,
        Instant windowStart,
        Instant windowEnd,
        List<Long> eventIds,
        String reviewedBy,
        Instant reviewedAt,
        String reviewerNote,
        Instant createdAt) {
    public static FlagView of(Flag flag) {
        return new FlagView(flag.getId(), flag.getSession().getId(), flag.getStatus().name(), flag.getReason(),
                flag.getConfidence(), flag.getWindowStart(), flag.getWindowEnd(), flag.getEventIds(),
                flag.getReviewedBy() == null ? null : flag.getReviewedBy().getFullName(),
                flag.getReviewedAt(), flag.getReviewerNote(), flag.getCreatedAt());
    }
}
