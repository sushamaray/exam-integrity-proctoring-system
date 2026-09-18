package com.uiu.examintegrity.proctoring.domain;

import java.util.Map;

public record StudentBaseline(Map<String, Double> factors) {
    public static final double FLOOR = 0.5;

    public StudentBaseline {
        factors = Map.copyOf(factors);
    }

    public static StudentBaseline neutral() {
        return new StudentBaseline(Map.of());
    }

    public double adjust(String detector, double confidence) {
        double factor = Math.max(FLOOR, Math.min(1.0, factors.getOrDefault(detector, 1.0)));
        return confidence * factor;
    }

    public static final int MIN_SITTINGS = 2;
    public static final double MIN_HOURS = 1.0;
    public static final long MIN_EVENTS = 10;

    public static final double PRIOR_HOURS = 1.0;

    public record History(long events, double hours, int sittings) {
        public double rate() {
            return hours > 0 ? events / hours : 0;
        }
    }

    public static double factorFor(History own, double cohortRate) {
        if (own.sittings() < MIN_SITTINGS || own.hours() < MIN_HOURS || own.events() < MIN_EVENTS) {
            return 1.0;
        }
        double shrunk = (own.events() + cohortRate * PRIOR_HOURS) / (own.hours() + PRIOR_HOURS);
        return factorFor(shrunk, cohortRate);
    }

    public static double factorFor(double studentRate, double cohortRate) {
        if (studentRate <= 0 || cohortRate <= 0 || studentRate <= cohortRate) {
            return 1.0;
        }
        return Math.max(FLOOR, cohortRate / studentRate);
    }
}
