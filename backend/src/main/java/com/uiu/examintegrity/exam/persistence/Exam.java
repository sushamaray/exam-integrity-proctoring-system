package com.uiu.examintegrity.exam.persistence;

import java.time.Instant;

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
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "questions_to_draw")
    private Integer questionsToDraw;

    @Column(name = "shuffle_questions", nullable = false)
    private boolean shuffleQuestions = true;

    @Column(name = "shuffle_options", nullable = false)
    private boolean shuffleOptions = true;

    @Column(name = "allow_autocomplete", nullable = false)
    private boolean allowAutocomplete = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public Exam(Course course, String title, Instant startsAt, Instant endsAt, int durationMinutes) {
        this.course = course;
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.durationMinutes = durationMinutes;
    }
}
