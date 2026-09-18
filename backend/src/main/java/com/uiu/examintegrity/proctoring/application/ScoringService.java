package com.uiu.examintegrity.proctoring.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.uiu.examintegrity.exam.domain.SessionClosed;
import com.uiu.examintegrity.exam.domain.SessionStarted;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.proctoring.domain.AggregateNode;
import com.uiu.examintegrity.proctoring.domain.BaselineCalibrated;
import com.uiu.examintegrity.proctoring.domain.Cluster;
import com.uiu.examintegrity.proctoring.domain.CorrelationEngine;
import com.uiu.examintegrity.proctoring.domain.Detector;
import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;
import com.uiu.examintegrity.proctoring.domain.RiskTier;
import com.uiu.examintegrity.proctoring.domain.RiskTreeBuilder;
import com.uiu.examintegrity.proctoring.domain.Signal;
import com.uiu.examintegrity.proctoring.domain.StudentBaseline;
import com.uiu.examintegrity.proctoring.persistence.Flag;
import com.uiu.examintegrity.proctoring.persistence.FlagRepository;
import com.uiu.examintegrity.proctoring.persistence.FlagStatus;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventMapper;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventRepository;
import com.uiu.examintegrity.proctoring.persistence.RiskScore;
import com.uiu.examintegrity.proctoring.persistence.RiskScoreRepository;

@Service
public class ScoringService {
    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private static final Set<FlagStatus> OPEN = EnumSet.of(FlagStatus.RAISED, FlagStatus.UNDER_REVIEW);

    private final WindowStore windowStore;
    private final List<Detector> detectors;
    private final BaselineService baselineService;
    private final ExamSessionRepository sessionRepository;
    private final ProctoringEventRepository eventRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final FlagRepository flagRepository;
    private final RiskUpdates updates;
    private final TransactionTemplate transaction;

    private final CorrelationEngine correlation = new CorrelationEngine();
    private final RiskTreeBuilder trees;
    private final Set<String> ruleViolations;

    public ScoringService(WindowStore windowStore, List<Detector> detectors, BaselineService baselineService,
            ExamSessionRepository sessionRepository, ProctoringEventRepository eventRepository,
            RiskScoreRepository riskScoreRepository, FlagRepository flagRepository, RiskUpdates updates,
            PlatformTransactionManager transactionManager) {
        this.windowStore = windowStore;
        this.detectors = detectors;
        this.baselineService = baselineService;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.flagRepository = flagRepository;
        this.updates = updates;
        this.transaction = new TransactionTemplate(transactionManager);
        this.ruleViolations = detectors.stream()
                .filter(Detector::isRuleViolation).map(Detector::name).collect(Collectors.toSet());
        this.trees = new RiskTreeBuilder(correlation, ruleViolations);
    }

    @Async("scoringExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventsPersisted(EventsPersisted persisted) {
        try {
            rescore(persisted.sessionId(), persisted.events());
        } catch (RuntimeException e) {
            log.error("Rescoring session {} failed", persisted.sessionId(), e);
        }
    }

    @Async("scoringExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionStarted(SessionStarted started) {
        updates.markDirty(started.examId(), started.sessionId());
    }

    @Async("scoringExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionClosed(SessionClosed closed) {
        windowStore.evict(closed.sessionId());
        updates.markDirty(closed.examId(), closed.sessionId());
    }

    public RiskAssessment rescore(Long sessionId, List<ProctoringEvent> newEvents) {
        ScoringContext context = windowStore.forSession(sessionId, () -> load(sessionId));
        if (context == null) {
            return null;
        }

        List<ProctoringEvent> ordered = new ArrayList<>(newEvents);
        ordered.sort(Comparator.comparing(ProctoringEvent::occurredAt));
        ordered.forEach(context::feed);

        RiskAssessment assessment = assess(context);
        transaction.executeWithoutResult(status -> persist(context, assessment));
        updates.markDirty(context.examId(), sessionId);
        return assessment;
    }

    private RiskAssessment assess(ScoringContext context) {
        List<Signal> signals = context.window().signals();
        List<Cluster> clusters = correlation.findClusters(signals);
        AggregateNode tree = trees.build(signals, clusters, context::questionLabel);
        double score = tree.score();
        return new RiskAssessment(score, RiskTier.of(score), tree, clusters);
    }

    private ScoringContext load(Long sessionId) {
        return transaction.execute(status -> {
            ExamSessionEntity session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null) {
                return null;
            }
            StudentBaseline baseline = baselineService.baselineFor(session.getUser().getId());

            List<Detector> calibrated = detectors.stream()
                    .<Detector>map(detector -> detector.isRuleViolation()
                            ? detector
                            : new BaselineCalibrated(detector, baseline))
                    .toList();

            Map<Long, Integer> positions = new HashMap<>();
            List<Long> order = session.getQuestionOrder() == null ? List.of() : session.getQuestionOrder();
            for (int i = 0; i < order.size(); i++) {
                positions.put(order.get(i), i + 1);
            }

            ScoringContext context = new ScoringContext(sessionId, session.getExam().getId(), calibrated, positions);
            ProctoringEventMapper.toDomain(eventRepository.findBySessionIdOrderByOccurredAt(sessionId))
                    .forEach(context::feed);
            return context;
        });
    }

    private void persist(ScoringContext context, RiskAssessment assessment) {
        Long sessionId = context.sessionId();
        RiskScore risk = riskScoreRepository.findById(sessionId)
                .orElseGet(() -> new RiskScore(sessionRepository.getReferenceById(sessionId)));
        risk.setScore(BigDecimal.valueOf(assessment.score()).setScale(2, RoundingMode.HALF_UP));
        risk.setTier(assessment.tier());
        risk.setBreakdown(RiskBreakdown.flatten(assessment.tree()));
        riskScoreRepository.save(risk);

        if (assessment.tier() == RiskTier.ESCALATE && !assessment.clusters().isEmpty()) {
            Cluster cluster = strongest(assessment.clusters());
            raiseFlagIfNew(sessionId, "CORRELATED_CLUSTER", cluster.signals());
        } else {
            context.window().signals().stream()
                    .filter(signal -> ruleViolations.contains(signal.detector()))
                    .reduce((first, second) -> second)
                    .ifPresent(latest -> raiseFlagIfNew(sessionId, latest.detector(), List.of(latest)));
        }
    }

    private void raiseFlagIfNew(Long sessionId, String reason, List<Signal> evidenceSignals) {
        Cluster cluster = new Cluster(evidenceSignals);
        if (flagRepository.findFirstBySessionIdAndStatusIn(sessionId, OPEN).isPresent()) {
            return;
        }
        var latest = flagRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId);
        if (latest.isPresent() && latest.get().getWindowEnd() != null
                && !cluster.start().isAfter(latest.get().getWindowEnd())) {
            return;
        }

        Set<Long> eventIds = new LinkedHashSet<>();
        cluster.signals().forEach(signal -> eventIds.addAll(signal.eventIds()));
        double confidence = cluster.signals().stream().mapToDouble(Signal::confidence).average().orElse(0);

        Flag flag = new Flag(sessionRepository.getReferenceById(sessionId), reason);
        flag.setConfidence(BigDecimal.valueOf(confidence).setScale(3, RoundingMode.HALF_UP));
        flag.setWindowStart(cluster.start());
        flag.setWindowEnd(cluster.end());
        flag.setEventIds(List.copyOf(eventIds));
        flagRepository.save(flag);
    }

    private Cluster strongest(List<Cluster> clusters) {
        return clusters.stream()
                .max(Comparator.comparingDouble(c -> c.signals().stream().mapToDouble(Signal::weighted).sum()
                        * correlation.multiplierFor(c)))
                .orElseThrow();
    }
}
