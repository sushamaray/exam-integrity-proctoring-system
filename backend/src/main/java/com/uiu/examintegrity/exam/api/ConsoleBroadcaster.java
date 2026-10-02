package com.uiu.examintegrity.exam.api;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.uiu.examintegrity.exam.application.view.ConsoleEvent;

/**
 * Pushes a running program's output to its student, mirroring
 * {@code proctoring.api.RiskBroadcaster}.
 *
 * <p>One topic per run rather than per session: the run id is what the WebSocket interceptor
 * resolves to an owner, and it keeps a stale subscription from picking up the next program.
 */
@Component
public class ConsoleBroadcaster {
    private final SimpMessagingTemplate messaging;

    public ConsoleBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public static String topicFor(String runId) {
        return "/topic/console/" + runId;
    }

    public void send(String runId, ConsoleEvent event) {
        messaging.convertAndSend(topicFor(runId), event);
    }
}
