package com.uiu.examintegrity.question;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findByQuestionIdOrderByLabel(Long questionId);

    List<QuestionOption> findByQuestionIdInOrderByLabel(Collection<Long> questionIds);

    Optional<QuestionOption> findByQuestionIdAndLabel(Long questionId, String label);
}
