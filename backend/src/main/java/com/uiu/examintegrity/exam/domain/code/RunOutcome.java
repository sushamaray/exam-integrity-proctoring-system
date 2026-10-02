package com.uiu.examintegrity.exam.domain.code;

import java.util.List;

/** The result of a whole run: an overall status plus one outcome per test case. */
public record RunOutcome(RunStatus status, String compileOutput, List<TestOutcome> tests, int runtimeMs) {
    public RunOutcome {
        tests = List.copyOf(tests);
    }

    public static RunOutcome compileError(String output) {
        return new RunOutcome(RunStatus.COMPILE_ERROR, output, List.of(), 0);
    }

    public static RunOutcome internalError(String output) {
        return new RunOutcome(RunStatus.INTERNAL_ERROR, output, List.of(), 0);
    }

    /**
     * The overall status is the first test that did not simply run -- a timeout anywhere is
     * worth surfacing above a plain wrong answer.
     */
    public static RunOutcome of(List<TestOutcome> tests, String compileOutput) {
        RunStatus overall = tests.stream()
                .map(TestOutcome::status)
                .filter(status -> !status.ran())
                .findFirst()
                .orElse(RunStatus.OK);
        int total = tests.stream().mapToInt(TestOutcome::runtimeMs).sum();
        return new RunOutcome(overall, compileOutput, tests, total);
    }

    public int passedCount() {
        return (int) tests.stream().filter(TestOutcome::passed).count();
    }

    public int totalCount() {
        return tests.size();
    }
}
