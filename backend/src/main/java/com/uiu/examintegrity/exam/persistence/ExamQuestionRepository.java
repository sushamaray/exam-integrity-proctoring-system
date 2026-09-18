package com.uiu.examintegrity.exam.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    List<ExamQuestion> findByExamId(Long examId);

    @Query("select eq from ExamQuestion eq join fetch eq.question where eq.exam.id = :examId order by eq.id")
    List<ExamQuestion> findPool(Long examId);

    long countByExamId(Long examId);
}
