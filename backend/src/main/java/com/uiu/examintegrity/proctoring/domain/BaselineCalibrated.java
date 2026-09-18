package com.uiu.examintegrity.proctoring.domain;

import java.util.Optional;

public final class BaselineCalibrated implements Detector {
    private final Detector delegate;
    private final StudentBaseline baseline;

    public BaselineCalibrated(Detector delegate, StudentBaseline baseline) {
        this.delegate = delegate;
        this.baseline = baseline;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public double baseWeight() {
        return delegate.baseWeight();
    }

    @Override
    public Optional<Signal> evaluate(SessionWindow window) {
        return delegate.evaluate(window)
                .map(signal -> signal.withConfidence(baseline.adjust(signal.detector(), signal.confidence())));
    }
}
