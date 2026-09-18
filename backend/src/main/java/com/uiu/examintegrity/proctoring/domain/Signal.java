package com.uiu.examintegrity.proctoring.domain;

import java.time.Instant;
import java.util.List;

public record Signal(
        String detector,
        double weight,
        double confidence,
        Instant at,
        Long questionId,
        List<Long> eventIds) {
    public Signal {
        confidence = Math.max(0, Math.min(1, confidence));
        eventIds = List.copyOf(eventIds);
    }

    public double weighted() {
        return weight * confidence;
    }

    public Signal withConfidence(double adjusted) {
        return new Signal(detector, weight, adjusted, at, questionId, eventIds);
    }
}
