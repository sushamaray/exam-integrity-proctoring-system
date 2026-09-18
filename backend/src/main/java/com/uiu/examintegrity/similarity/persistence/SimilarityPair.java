package com.uiu.examintegrity.similarity.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.uiu.examintegrity.exam.persistence.Exam;
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
@Table(name = "similarity_pairs")
@Getter
@Setter
@NoArgsConstructor
public class SimilarityPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_a_id", nullable = false)
    private ExamSessionEntity sessionA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_b_id", nullable = false)
    private ExamSessionEntity sessionB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Column(nullable = false, length = 32)
    private String method;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_spans", columnDefinition = "jsonb")
    private List<Map<String, Object>> matchedSpans;

    @Column(name = "computed_at", nullable = false, insertable = false, updatable = false)
    private Instant computedAt;

    public SimilarityPair(Exam exam, ExamSessionEntity sessionA, ExamSessionEntity sessionB,
            BigDecimal score, String method) {
        this.exam = exam;
        this.sessionA = sessionA;
        this.sessionB = sessionB;
        this.score = score;
        this.method = method;
    }
}
