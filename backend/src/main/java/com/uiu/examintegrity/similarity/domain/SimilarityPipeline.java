package com.uiu.examintegrity.similarity.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SimilarityPipeline {
    public final SimilarityReport run(Long examId) {
        List<SessionAnswers> answers = load(examId);
        List<SessionAnswers> normalized = normalize(answers);
        List<PairResult> pairs = comparePairs(normalized);
        List<PairResult> kept = applyThreshold(pairs);
        return cluster(examId, normalized, kept);
    }

    protected abstract List<SessionAnswers> load(Long examId);

    protected abstract List<PairResult> comparePairs(List<SessionAnswers> answers);

    protected abstract double thresholdFor(String method);

    protected List<SessionAnswers> normalize(List<SessionAnswers> answers) {
        List<SessionAnswers> out = new ArrayList<>(answers.size());
        for (SessionAnswers sitting : answers) {
            Map<Long, SessionAnswers.Answer> normalized = new HashMap<>();
            sitting.answers().forEach((q, answer) -> normalized.put(q,
                    answer.text() == null ? answer : answer.withNormalized(TextNormalizer.normalize(answer.text()))));
            out.add(new SessionAnswers(sitting.sessionId(), sitting.student(), normalized));
        }
        out.sort(Comparator.comparing(SessionAnswers::sessionId));
        return out;
    }

    protected List<PairResult> applyThreshold(List<PairResult> pairs) {
        return pairs.stream()
                .filter(pair -> pair.score() >= thresholdFor(pair.method()))
                .sorted(Comparator.comparingDouble(PairResult::score).reversed())
                .toList();
    }

    protected SimilarityReport cluster(Long examId, List<SessionAnswers> sittings, List<PairResult> edges) {
        UnionFind components = new UnionFind();
        edges.forEach(edge -> components.union(edge.pair().a(), edge.pair().b()));

        Map<Long, Integer> numbers = new HashMap<>();
        List<SimilarityReport.Node> nodes = new ArrayList<>();
        for (SessionAnswers sitting : sittings) {
            Long id = sitting.sessionId();
            Integer cluster = null;
            if (components.sizeOf(id) > 1) {
                cluster = numbers.computeIfAbsent(components.find(id), root -> numbers.size() + 1);
            }
            nodes.add(new SimilarityReport.Node(id, sitting.student(), cluster));
        }
        return new SimilarityReport(examId, nodes, edges);
    }
}
