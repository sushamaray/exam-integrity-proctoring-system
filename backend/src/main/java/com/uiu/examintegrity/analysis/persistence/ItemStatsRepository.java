package com.uiu.examintegrity.analysis.persistence;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ItemStatsRepository extends JpaRepository<ItemStats, Long> {
    List<ItemStats> findByExamId(Long examId);

    List<ItemStats> findByExamIdAndDiscriminationIndexLessThan(Long examId, BigDecimal threshold);

    /**
     * Recomputing replaces an exam's rows rather than appending a second set.
     *
     * <p>Spelled out as bulk DML rather than a derived {@code deleteByExamId}. A derived delete
     * loads the entities and queues their removal, and Hibernate flushes queued inserts <em>before</em>
     * queued deletes -- so the fresh rows collide with the old ones on
     * {@code UNIQUE (exam_id, question_id)} and recompute fails the second time it is run.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ItemStats s where s.exam.id = :examId")
    void deleteByExamId(Long examId);
}
