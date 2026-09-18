package com.uiu.examintegrity.proctoring.domain;

import java.util.Optional;

public interface Detector {
    String name();

    double baseWeight();

    Optional<Signal> evaluate(SessionWindow window);

    default boolean isRuleViolation() {
        return false;
    }
}
