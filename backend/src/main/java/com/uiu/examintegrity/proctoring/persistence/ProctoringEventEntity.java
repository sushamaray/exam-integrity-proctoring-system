package com.uiu.examintegrity.proctoring.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proctoring_events")
@Getter
@Setter
@NoArgsConstructor
public class ProctoringEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSessionEntity session;

    @Column(name = "client_event_id", nullable = false)
    private UUID clientEventId;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, insertable = false, updatable = false)
    private Instant receivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    public ProctoringEventEntity(ExamSessionEntity session, UUID clientEventId, String eventType, Instant occurredAt) {
        this.session = session;
        this.clientEventId = clientEventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }
}
