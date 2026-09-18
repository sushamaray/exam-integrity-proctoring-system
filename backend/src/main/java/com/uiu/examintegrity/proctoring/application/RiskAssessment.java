package com.uiu.examintegrity.proctoring.application;

import java.util.List;

import com.uiu.examintegrity.proctoring.domain.AggregateNode;
import com.uiu.examintegrity.proctoring.domain.Cluster;
import com.uiu.examintegrity.proctoring.domain.RiskTier;

public record RiskAssessment(double score, RiskTier tier, AggregateNode tree, List<Cluster> clusters) {
}
