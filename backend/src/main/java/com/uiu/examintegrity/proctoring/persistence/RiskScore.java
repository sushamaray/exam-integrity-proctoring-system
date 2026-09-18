package com.uiu.examintegrity.proctoring.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.proctoring.domain.RiskTier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "risk_scores")
@Getter
@Setter
@NoArgsConstructor
public class RiskScore {
    @Id
    @Column(name = "session_id")
    private Long sessionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "session_id")
    private ExamSessionEntity session;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiskTier tier = RiskTier.CLEAR;

    @Column(name = "cohort_percentile", precision = 5, scale = 2)
    private BigDecimal cohortPercentile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> breakdown = List.of();

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RiskScore(ExamSessionEntity session) {
        this.session = session;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
