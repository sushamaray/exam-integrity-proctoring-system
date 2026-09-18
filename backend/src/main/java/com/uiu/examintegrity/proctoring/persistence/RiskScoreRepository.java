package com.uiu.examintegrity.proctoring.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uiu.examintegrity.proctoring.domain.RiskTier;

public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {
    List<RiskScore> findBySessionExamIdOrderByScoreDesc(Long examId);

    List<RiskScore> findByTier(RiskTier tier);
}
