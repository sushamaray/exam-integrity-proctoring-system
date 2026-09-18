package com.uiu.examintegrity.proctoring.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class RiskUpdates {
    private final Map<Long, Set<Long>> dirtyByExam = new ConcurrentHashMap<>();

    public void markDirty(Long examId, Long sessionId) {
        dirtyByExam.computeIfAbsent(examId, id -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public Map<Long, Set<Long>> drain() {
        Map<Long, Set<Long>> drained = new HashMap<>();
        for (Long examId : dirtyByExam.keySet()) {
            Set<Long> sessions = dirtyByExam.remove(examId);
            if (sessions != null && !sessions.isEmpty()) {
                drained.put(examId, Set.copyOf(sessions));
            }
        }
        return drained;
    }
}
