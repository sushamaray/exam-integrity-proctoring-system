package com.uiu.examintegrity.proctoring.domain;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class Events {
    private static final AtomicLong IDS = new AtomicLong(1);

    private Events() {
    }

    public static Instant at(String time) {
        return Instant.parse("2026-09-18T" + time + "Z");
    }

    public static ProctoringEvent event(EventType type, Instant at) {
        return new ProctoringEvent(IDS.getAndIncrement(), type, at, at, null, Map.of());
    }

    public static ProctoringEvent event(EventType type, Instant at, Long questionId) {
        return new ProctoringEvent(IDS.getAndIncrement(), type, at, at, questionId, Map.of());
    }

    public static ProctoringEvent event(EventType type, String key, long value, Instant at) {
        return new ProctoringEvent(IDS.getAndIncrement(), type, at, at, null, Map.of(key, value));
    }

    public static Signal signal(String detector, double weight, double confidence, Instant at) {
        return new Signal(detector, weight, confidence, at, null, java.util.List.of(IDS.getAndIncrement()));
    }
}
