package com.uiu.examintegrity.similarity.domain;

import java.util.ArrayList;
import java.util.List;

import com.uiu.examintegrity.similarity.domain.SimilarityStrategy.PairScorer;

public abstract class StrategyPipeline extends SimilarityPipeline {
    private final List<SimilarityStrategy> strategies;

    protected StrategyPipeline(List<SimilarityStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    @Override
    protected List<PairResult> comparePairs(List<SessionAnswers> answers) {
        List<PairScorer> scorers = strategies.stream().map(strategy -> strategy.prepare(answers)).toList();
        List<PairResult> results = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            for (int j = i + 1; j < answers.size(); j++) {
                for (PairScorer scorer : scorers) {
                    results.addAll(scorer.score(answers.get(i), answers.get(j)));
                }
            }
        }
        return results;
    }

    @Override
    protected double thresholdFor(String method) {
        return strategies.stream()
                .filter(strategy -> strategy.method().equals(method))
                .mapToDouble(SimilarityStrategy::threshold)
                .findFirst()
                .orElse(1.0);
    }
}
