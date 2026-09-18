package com.uiu.examintegrity.exam.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "exam_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ExamSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SessionStatus status = SessionStatus.NOT_STARTED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_order", columnDefinition = "jsonb")
    private List<Long> questionOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_order", columnDefinition = "jsonb")
    private Map<String, List<String>> optionOrder;

    @Column(name = "consent_at")
    private Instant consentAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "total_marks", precision = 6, scale = 2)
    private BigDecimal totalMarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", nullable = false, length = 16)
    private GradingStatus gradingStatus = GradingStatus.PENDING;

    public ExamSessionEntity(Exam exam, User user) {
        this.exam = exam;
        this.user = user;
    }
}
