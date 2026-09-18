package com.uiu.examintegrity.proctoring.persistence;

import java.util.ArrayList;
import java.util.List;

import com.uiu.examintegrity.proctoring.domain.EventType;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;

public final class ProctoringEventMapper {
    private ProctoringEventMapper() {
    }

    public static List<ProctoringEvent> toDomain(List<ProctoringEventEntity> rows) {
        List<ProctoringEvent> events = new ArrayList<>(rows.size());
        for (ProctoringEventEntity row : rows) {
            EventType type;
            try {
                type = EventType.valueOf(row.getEventType());
            } catch (IllegalArgumentException unknown) {
                continue;
            }
            events.add(new ProctoringEvent(row.getId(), type, row.getOccurredAt(), row.getReceivedAt(),
                    row.getQuestion() == null ? null : row.getQuestion().getId(), row.getPayload()));
        }
        return events;
    }
}
