package com.uiu.examintegrity.exam.domain.code;

import java.util.List;

/** One unit of work for a {@link CodeExecutor}: source plus the cases to run it against. */
public record CodeRun(Language language, String sourceCode, List<TestCase> tests) {
    public CodeRun {
        tests = List.copyOf(tests);
    }
}
