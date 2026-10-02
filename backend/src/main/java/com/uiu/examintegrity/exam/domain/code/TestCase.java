package com.uiu.examintegrity.exam.domain.code;

/**
 * One case from {@code questions.test_cases}. A hidden case still decides the grade, but
 * its expected output never reaches the student.
 */
public record TestCase(String name, String input, String expected, boolean hidden) {
    public TestCase {
        input = input == null ? "" : input;
        expected = expected == null ? "" : expected;
    }
}
