package com.uiu.examintegrity.proctoring.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventWriter;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventWriter.Inserted;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventWriter.NewEvent;

@Service
public class EventIngestionService {
    private static final Duration FUTURE_TOLERANCE = Duration.ofSeconds(5);
    private static final int MAX_PAYLOAD_KEYS = 8;
    private static final int MAX_PAYLOAD_STRING = 64;

    private final ExamSessionRepository sessionRepository;
    private final ProctoringEventWriter writer;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public EventIngestionService(ExamSessionRepository sessionRepository, ProctoringEventWriter writer,
            ApplicationEventPublisher events, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.writer = writer;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public int accept(Long sessionId, Long userId, List<IncomingEvent> batch) {
        ExamSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("No such session"));
        if (!session.getUser().getId().equals(userId)) {
            throw new NotFoundException("No such session");
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ConflictException("This session is closed");
        }

        Instant now = clock.instant();
        Set<Long> onPaper = session.getQuestionOrder() == null
                ? Set.of()
                : new HashSet<>(session.getQuestionOrder());

        List<ProctoringEvent> inserted = new ArrayList<>();
        for (IncomingEvent incoming : batch) {
            Map<String, Object> payload = sanitise(incoming.payload());
            Instant occurredAt = clampTime(incoming.occurredAt(), session.getStartedAt(), now, payload);

            Long questionId = onPaper.contains(incoming.questionId()) ? incoming.questionId() : null;

            Inserted row = writer.insert(sessionId, new NewEvent(incoming.clientEventId(),
                    incoming.type().name(), occurredAt, questionId, payload));
            if (row != null) {
                inserted.add(new ProctoringEvent(row.id(), incoming.type(), occurredAt, row.receivedAt(),
                        questionId, payload));
            }
        }

        if (!inserted.isEmpty()) {
            events.publishEvent(new EventsPersisted(sessionId, session.getExam().getId(), inserted));
        }
        return inserted.size();
    }

    private static Instant clampTime(Instant claimed, Instant startedAt, Instant now,
            Map<String, Object> payload) {
        Instant ceiling = now.plus(FUTURE_TOLERANCE);
        Instant clamped = claimed;
        if (clamped.isAfter(ceiling)) {
            clamped = now;
        }
        if (startedAt != null && clamped.isBefore(startedAt)) {
            clamped = startedAt;
        }
        if (!clamped.equals(claimed)) {
            payload.put("clientOccurredAt", claimed.toString());
        }
        return clamped;
    }

    private static Map<String, Object> sanitise(Map<String, Object> raw) {
        Map<String, Object> clean = new LinkedHashMap<>();
        if (raw == null) {
            return clean;
        }
        for (var entry : raw.entrySet()) {
            if (clean.size() >= MAX_PAYLOAD_KEYS || entry.getKey().length() > MAX_PAYLOAD_STRING) {
                break;
            }
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean
                    || (value instanceof String s && s.length() <= MAX_PAYLOAD_STRING)) {
                clean.put(entry.getKey(), value);
            }
        }
        return clean;
    }
}
