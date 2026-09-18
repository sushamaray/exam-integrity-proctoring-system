package com.uiu.examintegrity.exam.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface ExamSessionRepository extends JpaRepository<ExamSessionEntity, Long> {
    Optional<ExamSessionEntity> findByExamIdAndUserId(Long examId, Long userId);

    List<ExamSessionEntity> findByUserId(Long userId);

    List<ExamSessionEntity> findByUserIdAndExamIdIn(Long userId, Collection<Long> examIds);

    List<ExamSessionEntity> findByExamId(Long examId);

    @Query("select s from ExamSessionEntity s join fetch s.user where s.exam.id = :examId")
    List<ExamSessionEntity> findWithUserByExamId(Long examId);

    @Query("select distinct s.exam.id from ExamSessionEntity s where s.status = :status")
    List<Long> findExamIdsWithStatus(SessionStatus status);

    long countByExamId(Long examId);

    long countByExamIdAndStatus(Long examId, SessionStatus status);

    List<ExamSessionEntity> findByExamIdAndStatus(Long examId, SessionStatus status);

    boolean existsByExamIdAndStatus(Long examId, SessionStatus status);

    List<ExamSessionEntity> findByStatusAndExpiresAtBefore(SessionStatus status, Instant cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ExamSessionEntity s where s.id = :id")
    Optional<ExamSessionEntity> findLockedById(Long id);
}
