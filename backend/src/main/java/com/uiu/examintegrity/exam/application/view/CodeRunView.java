package com.uiu.examintegrity.exam.application.view;

import java.util.List;

import com.uiu.examintegrity.exam.domain.code.RunOutcome;
import com.uiu.examintegrity.exam.domain.code.TestOutcome;

/**
 * What the editor is told about a run.
 *
 * <p>A hidden case contributes its name and whether it passed, and nothing else. Mapping it
 * through {@link TestResultView#of} is the only place that decision is made, because leaking
 * a hidden expected output here would hand the student the answer key.
 */
public record CodeRunView(
        Long submissionId,
        String language,
        String status,
        boolean isFinal,
        String compileOutput,
        int passed,
        int total,
        int runtimeMs,
        List<TestResultView> tests) {

    public record TestResultView(
            String name,
            boolean hidden,
            boolean passed,
            String status,
            String stdout,
            String stderr,
            int runtimeMs) {

        static TestResultView of(TestOutcome outcome) {
            if (outcome.hidden()) {
                return new TestResultView(outcome.name(), true, outcome.passed(),
                        outcome.status().name(), null, null, outcome.runtimeMs());
            }
            return new TestResultView(outcome.name(), false, outcome.passed(), outcome.status().name(),
                    outcome.stdout(), outcome.stderr(), outcome.runtimeMs());
        }
    }

    public static CodeRunView of(Long submissionId, String language, boolean isFinal, RunOutcome outcome) {
        return new CodeRunView(submissionId, language, outcome.status().name(), isFinal,
                outcome.compileOutput(), outcome.passedCount(), outcome.totalCount(), outcome.runtimeMs(),
                outcome.tests().stream().map(TestResultView::of).toList());
    }
}
