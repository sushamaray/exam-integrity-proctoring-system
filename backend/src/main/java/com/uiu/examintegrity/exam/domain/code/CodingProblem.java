package com.uiu.examintegrity.exam.domain.code;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A coding question's runnable part, read out of the loosely-typed
 * {@code questions.test_cases} jsonb. Keeping the map-munging here means both the exam view
 * and the runner agree on what a test case is.
 */
public record CodingProblem(Long questionId, String starterCode, List<TestCase> tests) {
    public CodingProblem {
        tests = List.copyOf(tests);
    }

    public static CodingProblem from(Long questionId, String starterCode, List<Map<String, Object>> raw) {
        List<TestCase> parsed = new ArrayList<>();
        if (raw != null) {
            for (int i = 0; i < raw.size(); i++) {
                Map<String, Object> row = raw.get(i);
                if (row == null) {
                    continue;
                }
                boolean hidden = Boolean.TRUE.equals(row.get("hidden"));
                String name = text(row.get("name"));
                parsed.add(new TestCase(
                        name.isBlank() ? (hidden ? "Hidden case " + (i + 1) : "Case " + (i + 1)) : name,
                        text(row.get("input")),
                        text(row.get("expected")),
                        hidden));
            }
        }
        return new CodingProblem(questionId, starterCode, parsed);
    }

    public List<TestCase> visibleTests() {
        return tests.stream().filter(test -> !test.hidden()).toList();
    }

    public boolean runnable() {
        return !tests.isEmpty();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
