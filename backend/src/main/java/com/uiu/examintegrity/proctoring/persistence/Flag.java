package com.uiu.examintegrity.proctoring.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "flags")
@Getter
@Setter
@NoArgsConstructor
public class Flag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSessionEntity session;

    @Column(nullable = false, length = 64)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FlagStatus status = FlagStatus.RAISED;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "window_end")
    private Instant windowEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> eventIds = List.of();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewer_note", columnDefinition = "text")
    private String reviewerNote;

    @Column(name = "student_note", columnDefinition = "text")
    private String studentNote;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public Flag(ExamSessionEntity session, String reason) {
        this.session = session;
        this.reason = reason;
    }
}
