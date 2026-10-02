package com.uiu.examintegrity.analysis.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.question.Question;

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
@Table(name = "item_stats")
@Getter
@Setter
@NoArgsConstructor
public class ItemStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "difficulty_index", precision = 5, scale = 4)
    private BigDecimal difficultyIndex;

    @Column(name = "discrimination_index", precision = 5, scale = 4)
    private BigDecimal discriminationIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_distribution", columnDefinition = "jsonb")
    private Map<String, BigDecimal> optionDistribution;

    @Column(name = "avg_seconds", precision = 8, scale = 2)
    private BigDecimal avgSeconds;

    /**
     * Set by the service rather than left to the column default, because recompute reads the view
     * back in the same transaction and a database-generated value would still be null there.
     */
    @Column(name = "computed_at", nullable = false, updatable = false)
    private Instant computedAt;

    public ItemStats(Exam exam, Question question, int attempts, int correctCount) {
        this.exam = exam;
        this.question = question;
        this.attempts = attempts;
        this.correctCount = correctCount;
    }
}
