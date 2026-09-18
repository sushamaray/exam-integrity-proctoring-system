package com.uiu.examintegrity.exam.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "code_submissions")
@Getter
@Setter
@NoArgsConstructor
public class CodeSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSessionEntity session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, length = 32)
    private String language;

    @Column(name = "source_code", nullable = false, columnDefinition = "text")
    private String sourceCode;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal;

    @Column(name = "judge0_token", length = 64)
    private String judge0Token;

    @Column(name = "passed_count")
    private Integer passedCount;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> result;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public CodeSubmission(ExamSessionEntity session, Question question, String language, String sourceCode) {
        this.session = session;
        this.question = question;
        this.language = language;
        this.sourceCode = sourceCode;
    }
}
