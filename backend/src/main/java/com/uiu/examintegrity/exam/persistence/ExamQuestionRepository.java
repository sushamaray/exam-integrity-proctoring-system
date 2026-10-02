package com.uiu.examintegrity.exam.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.uiu.examintegrity.question.QuestionType;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    List<ExamQuestion> findByExamId(Long examId);

    @Query("select eq from ExamQuestion eq join fetch eq.question where eq.exam.id = :examId order by eq.id")
    List<ExamQuestion> findPool(Long examId);

    long countByExamId(Long examId);

    /**
     * Which of these exams hold at least one question of this type. The dashboard uses it to
     * send a coding exam to the editor rather than the objective runner, in one query instead
     * of a pool lookup per exam.
     */
    @Query("select distinct eq.exam.id from ExamQuestion eq "
            + "where eq.exam.id in :examIds and eq.question.type = :type")
    List<Long> findExamIdsHolding(Collection<Long> examIds, QuestionType type);
}
