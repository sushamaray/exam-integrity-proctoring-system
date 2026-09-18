package com.uiu.examintegrity.analysis;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemStatsRepository extends JpaRepository<ItemStats, Long> {
    List<ItemStats> findByExamId(Long examId);

    List<ItemStats> findByExamIdAndDiscriminationIndexLessThan(Long examId, BigDecimal threshold);
}
