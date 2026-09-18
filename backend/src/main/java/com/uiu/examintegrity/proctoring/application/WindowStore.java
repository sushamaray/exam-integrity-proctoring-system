package com.uiu.examintegrity.proctoring.application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

@Component
public class WindowStore {
    private final Map<Long, ScoringContext> contexts = new ConcurrentHashMap<>();

    ScoringContext forSession(Long sessionId, Supplier<ScoringContext> load) {
        ScoringContext context = contexts.get(sessionId);
        if (context == null) {
            context = load.get();
            if (context != null) {
                contexts.put(sessionId, context);
            }
        }
        return context;
    }

    public void evict(Long sessionId) {
        contexts.remove(sessionId);
    }

    public int size() {
        return contexts.size();
    }
}
