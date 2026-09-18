package com.uiu.examintegrity.proctoring.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProctoringEventRepository extends JpaRepository<ProctoringEventEntity, Long> {
    List<ProctoringEventEntity> findBySessionIdOrderByOccurredAt(Long sessionId);

    List<ProctoringEventEntity> findTop50BySessionIdOrderByOccurredAtDesc(Long sessionId);

    List<ProctoringEventEntity> findBySessionIdAndEventType(Long sessionId, String eventType);

    boolean existsBySessionIdAndClientEventId(Long sessionId, UUID clientEventId);

    long countBySessionId(Long sessionId);
}
