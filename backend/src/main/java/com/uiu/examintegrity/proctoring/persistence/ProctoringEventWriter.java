package com.uiu.examintegrity.proctoring.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.json.JsonMapper;

@Repository
public class ProctoringEventWriter {
    private static final String INSERT = """
            INSERT INTO proctoring_events
                   (session_id, client_event_id, event_type, occurred_at, question_id, payload)
            VALUES (?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (session_id, client_event_id) DO NOTHING
            RETURNING id, received_at
            """;

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public ProctoringEventWriter(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public record NewEvent(UUID clientEventId, String type, Instant occurredAt, Long questionId,
            Map<String, Object> payload) {
    }

    public record Inserted(Long id, Instant receivedAt) {
    }

    public Inserted insert(Long sessionId, NewEvent event) {
        List<Inserted> rows = jdbc.query(INSERT,
                (rs, i) -> new Inserted(rs.getLong("id"), rs.getTimestamp("received_at").toInstant()),
                sessionId, event.clientEventId(), event.type(), Timestamp.from(event.occurredAt()),
                event.questionId(), toJson(event.payload()));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String toJson(Map<String, Object> payload) {
        return json.writeValueAsString(payload == null ? Map.of() : payload);
    }
}
