package com.uiu.examintegrity.proctoring.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.uiu.examintegrity.proctoring.domain.EventType;

public record IncomingEvent(UUID clientEventId, EventType type, Instant occurredAt, Long questionId,
        Map<String, Object> payload) {
}
