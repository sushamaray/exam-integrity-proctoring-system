package com.uiu.examintegrity.proctoring.domain;

import static com.uiu.examintegrity.proctoring.domain.EventType.HEARTBEAT;
import static com.uiu.examintegrity.proctoring.domain.EventType.PASTE;
import static com.uiu.examintegrity.proctoring.domain.Events.at;
import static com.uiu.examintegrity.proctoring.domain.Events.event;
import static com.uiu.examintegrity.proctoring.domain.Events.signal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uiu.examintegrity.proctoring.domain.detectors.LargePasteDetector;

class ScoringModelTest {
    private final CorrelationEngine correlation = new CorrelationEngine();
    private final RiskTreeBuilder trees = new RiskTreeBuilder(correlation);

    @Test
    @DisplayName("three signals inside a minute are a cluster")
    void clusterWithinSpan() {
        List<Signal> signals = List.of(
                signal("LARGE_PASTE", 0.45, 0.9, at("14:41:00")),
                signal("FOCUS_LOSS", 0.30, 0.6, at("14:41:20")),
                signal("FULLSCREEN_EXIT", 0.25, 0.5, at("14:41:50")));
        List<Cluster> clusters = correlation.findClusters(signals);
        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).size()).isEqualTo(3);
    }

    @Test
    @DisplayName("the same three signals spread over minutes are not")
    void noClusterWhenSpread() {
        List<Signal> signals = List.of(
                signal("LARGE_PASTE", 0.45, 0.9, at("14:41:00")),
                signal("FOCUS_LOSS", 0.30, 0.6, at("14:43:00")),
                signal("FULLSCREEN_EXIT", 0.25, 0.5, at("14:45:00")));
        assertThat(correlation.findClusters(signals)).isEmpty();
    }

    @Test
    @DisplayName("a sweep finds separate clusters and puts each signal in at most one")
    void twoClusters() {
        List<Signal> signals = List.of(
                signal("A", 0.3, 0.5, at("14:00:00")), signal("B", 0.3, 0.5, at("14:00:10")),
                signal("C", 0.3, 0.5, at("14:00:20")), signal("D", 0.3, 0.5, at("14:00:30")),
                signal("E", 0.3, 0.5, at("14:10:00")), signal("F", 0.3, 0.5, at("14:10:05")),
                signal("G", 0.3, 0.5, at("14:10:09")));
        List<Cluster> clusters = correlation.findClusters(signals);
        assertThat(clusters).extracting(Cluster::size).containsExactly(4, 3);
    }

    @Test
    @DisplayName("a cluster is worth more than its parts; isolated signals count half")
    void superadditiveVersusIsolated() {
        List<Signal> clustered = List.of(
                signal("LARGE_PASTE", 0.45, 0.9, at("14:41:00")),
                signal("FOCUS_LOSS", 0.30, 0.6, at("14:41:20")),
                signal("FULLSCREEN_EXIT", 0.25, 0.5, at("14:41:50")));
        double parts = clustered.stream().mapToDouble(s -> s.weighted() * 100).sum();

        AggregateNode together = trees.build(clustered, correlation.findClusters(clustered), q -> "Q");
        assertThat(together.score()).isCloseTo(parts * 1.15, within(1e-9));

        List<Signal> spread = List.of(
                signal("LARGE_PASTE", 0.45, 0.9, at("14:01:00")),
                signal("FOCUS_LOSS", 0.30, 0.6, at("14:21:20")),
                signal("FULLSCREEN_EXIT", 0.25, 0.5, at("14:41:50")));
        AggregateNode apart = trees.build(spread, correlation.findClusters(spread), q -> "Q");
        assertThat(apart.score()).isCloseTo(parts * RiskTreeBuilder.ISOLATED_DISCOUNT, within(1e-9));

        assertThat(RiskTier.of(together.score())).isEqualTo(RiskTier.ESCALATE);
        assertThat(RiskTier.of(apart.score())).isEqualTo(RiskTier.REVIEW);
    }

    @Test
    @DisplayName("a broken rule on its own counts in full, where other isolated signals count half")
    void ruleViolationsAreNotDiscounted() {
        RiskTreeBuilder rules = new RiskTreeBuilder(correlation, Set.of("TAB_SWITCH"));
        List<Signal> one = List.of(signal("TAB_SWITCH", 0.5, 0.9, at("14:41:00")));
        AggregateNode root = rules.build(one, correlation.findClusters(one), q -> "Q");

        assertThat(root.score()).isCloseTo(45, within(1e-9));
        assertThat(((AggregateNode) root.children().get(0)).kind()).isEqualTo("RULE");
        assertThat(trees.build(one, List.of(), q -> "Q").score()).isCloseTo(22.5, within(1e-9));
    }

    @Test
    @DisplayName("the tree is capped at 100 and its leaves carry their signals")
    void treeShape() {
        List<Signal> many = List.of(
                signal("A", 0.9, 1.0, at("14:00:00")), signal("B", 0.9, 1.0, at("14:00:05")),
                signal("C", 0.9, 1.0, at("14:00:10")), signal("D", 0.9, 1.0, at("14:00:15")));
        AggregateNode root = trees.build(many, correlation.findClusters(many), q -> "Q");
        assertThat(root.score()).isEqualTo(100);
        AggregateNode cluster = (AggregateNode) root.children().get(0);
        assertThat(cluster.kind()).isEqualTo("CLUSTER");
        assertThat(cluster.children()).allMatch(SignalNode.class::isInstance);
    }

    @Test
    @DisplayName("the baseline decorator lowers confidence for a habitual behaviour and leaves others alone")
    void baselineCalibration() {
        var window = SessionWindow.of(event(PASTE, "charCount", 240, at("14:41:02")));
        double raw = new LargePasteDetector().evaluate(window).orElseThrow().confidence();

        var habitual = new StudentBaseline(Map.of("LARGE_PASTE", 0.6));
        double calibrated = new BaselineCalibrated(new LargePasteDetector(), habitual)
                .evaluate(window).orElseThrow().confidence();
        double untouched = new BaselineCalibrated(new LargePasteDetector(), StudentBaseline.neutral())
                .evaluate(window).orElseThrow().confidence();

        assertThat(calibrated).isCloseTo(raw * 0.6, within(1e-9));
        assertThat(untouched).isEqualTo(raw);
    }

    @Test
    @DisplayName("a baseline only ever discounts, and never below the floor")
    void baselineFactorBounds() {
        assertThat(StudentBaseline.factorFor(1, 4)).isEqualTo(1.0);
        assertThat(StudentBaseline.factorFor(8, 4)).isEqualTo(0.5);
        assertThat(StudentBaseline.factorFor(100, 4)).isEqualTo(StudentBaseline.FLOOR);
        assertThat(StudentBaseline.factorFor(5, 0)).isEqualTo(1.0);
        assertThat(new StudentBaseline(Map.of("X", 0.1)).adjust("X", 1.0)).isEqualTo(StudentBaseline.FLOOR);
    }

    @Test
    @DisplayName("a thin history is not a habit; a long one is, but is still pulled towards the cohort")
    void baselineNeedsEvidence() {
        assertThat(StudentBaseline.factorFor(new StudentBaseline.History(8, 0.63, 1), 3.72)).isEqualTo(1.0);
        assertThat(StudentBaseline.factorFor(new StudentBaseline.History(40, 3.0, 2), 0)).isEqualTo(1.0);

        double habitual = StudentBaseline.factorFor(new StudentBaseline.History(36, 3.0, 3), 8.0);
        double unshrunk = StudentBaseline.factorFor(12.0, 8.0);
        assertThat(habitual).isLessThan(1.0).isGreaterThan(unshrunk);
    }

    @Test
    @DisplayName("re-running a detector over the same evidence records it once")
    void signalsAreNotDoubleCounted() {
        var window = SessionWindow.of(event(PASTE, "charCount", 240, at("14:41:02")));
        var detector = new LargePasteDetector();
        detector.evaluate(window).ifPresent(window::recordSignal);
        window.append(event(HEARTBEAT, at("14:41:05")));
        detector.evaluate(window).ifPresent(window::recordSignal);
        assertThat(window.signals()).hasSize(1);
    }

    @Test
    @DisplayName("a growing burst of pastes merges into one signal covering all of them")
    void overlappingEvidenceMerges() {
        var detector = new LargePasteDetector();
        var window = SessionWindow.of(event(PASTE, "charCount", 200, at("14:41:00")));
        detector.evaluate(window).ifPresent(window::recordSignal);
        window.append(event(PASTE, "charCount", 600, at("14:41:10")));
        detector.evaluate(window).ifPresent(window::recordSignal);

        assertThat(window.signals()).hasSize(1);
        assertThat(window.signals().get(0).eventIds()).hasSize(2);
    }

    @Test
    @DisplayName("pruning drops old events but keeps the signals raised from them")
    void pruningKeepsSignals() {
        var window = SessionWindow.of(event(PASTE, "charCount", 240, at("14:00:00")));
        new LargePasteDetector().evaluate(window).ifPresent(window::recordSignal);
        window.append(event(HEARTBEAT, at("14:30:00")));
        window.prune(Duration.ofMinutes(5));

        assertThat(window.events()).hasSize(1);
        assertThat(window.signals()).hasSize(1);
    }

    @Test
    @DisplayName("an event id seen before is ignored")
    void duplicateEventsIgnored() {
        var paste = event(PASTE, "charCount", 240, at("14:41:02"));
        var window = SessionWindow.of(paste, paste);
        assertThat(window.events()).hasSize(1);
    }
}
