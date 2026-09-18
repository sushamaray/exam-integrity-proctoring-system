package com.uiu.examintegrity.proctoring.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.uiu.examintegrity.proctoring.application.IncomingEvent;
import com.uiu.examintegrity.proctoring.domain.EventType;

import jakarta.validation.constraints.NotNull;

public record EventDto(
        @NotNull UUID clientEventId,
        @NotNull EventType type,
        @NotNull Instant occurredAt,
        Long questionId,
        Map<String, Object> payload) {
    public IncomingEvent toIncoming() {
        return new IncomingEvent(clientEventId, type, occurredAt, questionId, payload);
    }
}
