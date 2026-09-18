package com.uiu.examintegrity.proctoring.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FlagRepository extends JpaRepository<Flag, Long> {
    List<Flag> findByStatusOrderByCreatedAtDesc(FlagStatus status);

    List<Flag> findByStatusInOrderByCreatedAtDesc(List<FlagStatus> statuses);

    List<Flag> findBySessionId(Long sessionId);

    Optional<Flag> findFirstBySessionIdAndStatusIn(Long sessionId, Collection<FlagStatus> statuses);

    Optional<Flag> findFirstBySessionIdOrderByCreatedAtDesc(Long sessionId);

    List<Flag> findBySessionExamIdAndStatusIn(Long examId, Collection<FlagStatus> statuses);

    @Query("""
            select f from Flag f
              join fetch f.session s
              join fetch s.user
              join fetch s.exam e
             where f.status in :statuses and e.course.id in :courseIds
             order by f.createdAt desc
            """)
    List<Flag> findQueue(Collection<FlagStatus> statuses, Collection<Long> courseIds);
}
