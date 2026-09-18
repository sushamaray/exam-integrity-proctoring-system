package com.uiu.examintegrity.proctoring.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.uiu.examintegrity.proctoring.domain.StudentBaseline;

@Service
public class BaselineService {
    private static final Map<String, List<String>> CALIBRATED = Map.of(
            "FOCUS_LOSS", List.of("WINDOW_BLUR"),
            "FULLSCREEN_EXIT", List.of("FS_EXIT"));

    private static final String OBSERVED = "s.status IN ('SUBMITTED', 'AUTO_SUBMITTED')"
            + " AND s.started_at IS NOT NULL AND s.submitted_at > s.started_at"
            + " AND EXISTS (SELECT 1 FROM proctoring_events any_e WHERE any_e.session_id = s.id)";

    private final JdbcTemplate jdbc;

    public BaselineService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public StudentBaseline baselineFor(Long userId) {
        Map<String, Double> factors = new HashMap<>();
        for (var entry : CALIBRATED.entrySet()) {
            StudentBaseline.History own = history(entry.getValue(), "s.user_id = ?", userId);
            StudentBaseline.History cohort = history(entry.getValue(), "s.user_id <> ?", userId);
            double factor = StudentBaseline.factorFor(own, cohort.rate());
            if (factor < 1.0) {
                factors.put(entry.getKey(), factor);
            }
        }
        return new StudentBaseline(factors);
    }

    private StudentBaseline.History history(List<String> types, String who, Long userId) {
        String placeholders = String.join(",", types.stream().map(t -> "?").toList());

        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.addAll(types);
        args.add(userId);

        return jdbc.queryForObject("""
                SELECT events.n, sittings.h, sittings.c
                  FROM (SELECT COUNT(*) AS n
                          FROM proctoring_events e JOIN exam_sessions s ON s.id = e.session_id
                         WHERE %1$s AND %2$s AND e.event_type IN (%3$s)) events,
                       (SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (s.submitted_at - s.started_at))), 0) / 3600.0 AS h,
                               COUNT(*) AS c
                          FROM exam_sessions s
                         WHERE %1$s AND %2$s) sittings
                """.formatted(who, OBSERVED, placeholders),
                (rs, i) -> new StudentBaseline.History(rs.getLong("n"), rs.getDouble("h"), rs.getInt("c")),
                args.toArray());
    }
}
