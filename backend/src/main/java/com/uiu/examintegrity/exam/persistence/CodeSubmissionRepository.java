package com.uiu.examintegrity.exam.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {
    List<CodeSubmission> findBySessionIdOrderByCreatedAt(Long sessionId);

    List<CodeSubmission> findBySessionIdAndQuestionIdOrderByCreatedAt(Long sessionId, Long questionId);
}
