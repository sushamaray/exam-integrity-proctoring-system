package com.uiu.examintegrity.proctoring.domain;

public sealed interface RiskNode permits SignalNode, AggregateNode {
    String label();

    double score();
}
