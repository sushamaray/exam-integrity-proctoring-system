package com.uiu.examintegrity.proctoring.domain;

import java.time.Instant;
import java.util.Map;

public record ProctoringEvent(
        Long id,
        EventType type,
        Instant occurredAt,
        Instant receivedAt,
        Long questionId,
        Map<String, Object> payload) {
    public ProctoringEvent {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public long payloadLong(String key, long fallback) {
        return payload.get(key) instanceof Number n ? n.longValue() : fallback;
    }

    public int payloadInt(String key) {
        return (int) payloadLong(key, 0);
    }
}
