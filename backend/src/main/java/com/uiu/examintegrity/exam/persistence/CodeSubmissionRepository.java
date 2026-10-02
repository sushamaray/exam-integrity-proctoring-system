package com.uiu.examintegrity.exam.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {
    List<CodeSubmission> findBySessionIdOrderByCreatedAt(Long sessionId);

    List<CodeSubmission> findBySessionIdAndQuestionIdOrderByCreatedAt(Long sessionId, Long questionId);

    /**
     * Every final submission for a session, newest first. Grading walks this once and keeps the
     * first hit per question, rather than issuing a query per coding question.
     *
     * <p>Spelled out as JPQL because a derived name would have to read {@code AndIsFinalTrue},
     * where Spring Data cannot tell the {@code Is} keyword from the {@code isFinal} property.
     */
    @Query("select s from CodeSubmission s where s.session.id = :sessionId and s.isFinal = true "
            + "order by s.createdAt desc")
    List<CodeSubmission> findFinalBySessionId(Long sessionId);
}
