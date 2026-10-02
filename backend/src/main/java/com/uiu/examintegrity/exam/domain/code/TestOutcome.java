package com.uiu.examintegrity.exam.domain.code;

import java.util.stream.Collectors;

/** What happened for one test case. */
public record TestOutcome(
        String name,
        boolean hidden,
        boolean passed,
        RunStatus status,
        String stdout,
        String stderr,
        int runtimeMs) {

    public static TestOutcome of(TestCase test, RunStatus status, String stdout, String stderr, int runtimeMs) {
        boolean passed = status.ran() && matches(stdout, test.expected());
        return new TestOutcome(test.name(), test.hidden(), passed, status, stdout, stderr, runtimeMs);
    }

    /**
     * Trailing whitespace and line-ending differences are not wrong answers -- MinGW writes
     * CRLF while the expected output in the question bank is written with LF.
     */
    static boolean matches(String actual, String expected) {
        return normalise(actual).equals(normalise(expected));
    }

    private static String normalise(String text) {
        if (text == null) {
            return "";
        }
        String unix = text.replace("\r\n", "\n").replace('\r', '\n');
        return unix.lines().map(String::stripTrailing).collect(Collectors.joining("\n")).strip();
    }
}
