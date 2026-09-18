package com.uiu.examintegrity.question;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByCourseId(Long courseId);

    List<Question> findByCourseIdAndType(Long courseId, QuestionType type);
}
