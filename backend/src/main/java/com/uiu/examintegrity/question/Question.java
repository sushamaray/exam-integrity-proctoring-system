package com.uiu.examintegrity.question;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.uiu.examintegrity.course.Course;
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
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestionType type;

    @Column(length = 120)
    private String topic;

    @Column(name = "bloom_level", length = 24)
    private String bloomLevel;

    private Short difficulty;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks = BigDecimal.ONE;

    @Column(name = "starter_code", columnDefinition = "text")
    private String starterCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "test_cases", columnDefinition = "jsonb")
    private List<Map<String, Object>> testCases;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public Question(Course course, QuestionType type, String body, BigDecimal maxMarks) {
        this.course = course;
        this.type = type;
        this.body = body;
        this.maxMarks = maxMarks;
    }
}
