package com.uiu.examintegrity.exam.api;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * Remembers which console topics somebody is actually listening to.
 *
 * <p>This exists because the broker has no replay and <em>no SUBSCRIBE receipt</em>: Spring's
 * simple broker acknowledges nothing, so a client cannot tell when its subscription went live, and
 * a program launched a moment too early prints its first prompt into the void. Rather than guess
 * on the client, the server waits here for the subscription before it starts the program.
 *
 * <p>Fed by {@link SessionSubscribeEvent}, which Spring publishes once a SUBSCRIBE has been
 * handled -- later than the authorizing interceptor sees it, which is exactly the point.
 */
@Component
public class ConsoleSubscriptions {
    private static final Pattern CONSOLE_TOPIC = Pattern.compile("^/topic/console/([0-9a-fA-F-]{36})$");

    private static final long POLL_MS = 25;

    private final Set<String> watched = ConcurrentHashMap.newKeySet();

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        Matcher console = CONSOLE_TOPIC.matcher(destination);
        if (console.matches()) {
            watched.add(console.group(1));
        }
    }

    public boolean isWatched(String runId) {
        return watched.contains(runId);
    }

    /**
     * Waits for somebody to be listening to this run.
     *
     * @return true if a subscriber turned up within the timeout. False is not fatal -- the caller
     *         starts the program anyway, since refusing to run because a socket is slow would be
     *         worse than losing the first line of output.
     */
    public boolean awaitSubscriber(String runId, long timeoutMs) {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        while (!isWatched(runId)) {
            if (System.nanoTime() > deadline) {
                return false;
            }
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    public void forget(String runId) {
        watched.remove(runId);
    }
}
