package com.uiu.examintegrity.proctoring.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.proctoring.persistence.RiskScore;
import com.uiu.examintegrity.proctoring.persistence.RiskScoreRepository;

@Service
public class CohortBaselineService {
    private static final Logger log = LoggerFactory.getLogger(CohortBaselineService.class);

    private final ExamSessionRepository sessionRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final RiskUpdates updates;
    private final Executor scoringExecutor;
    private final TransactionTemplate transaction;

    public CohortBaselineService(ExamSessionRepository sessionRepository, RiskScoreRepository riskScoreRepository,
            RiskUpdates updates, @Qualifier("scoringExecutor") Executor scoringExecutor,
            PlatformTransactionManager transactionManager) {
        this.sessionRepository = sessionRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.updates = updates;
        this.scoringExecutor = scoringExecutor;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${proctoring.cohort.interval-ms:30000}")
    public void schedule() {
        scoringExecutor.execute(() -> {
            try {
                recomputeLiveExams();
            } catch (RuntimeException e) {
                log.error("Cohort percentile recompute failed", e);
            }
        });
    }

    public void recomputeLiveExams() {
        for (Long examId : sessionRepository.findExamIdsWithStatus(SessionStatus.IN_PROGRESS)) {
            transaction.executeWithoutResult(status -> recompute(examId));
        }
    }

    void recompute(Long examId) {
        List<RiskScore> scores = riskScoreRepository.findBySessionExamIdOrderByScoreDesc(examId);
        int n = scores.size();
        for (RiskScore risk : scores) {
            long below = scores.stream().filter(other -> other.getScore().compareTo(risk.getScore()) < 0).count();
            BigDecimal percentile = BigDecimal.valueOf(below * 100.0 / n).setScale(2, RoundingMode.HALF_UP);
            if (risk.getCohortPercentile() == null || risk.getCohortPercentile().compareTo(percentile) != 0) {
                risk.setCohortPercentile(percentile);
                updates.markDirty(examId, risk.getSessionId());
            }
        }
    }
}
