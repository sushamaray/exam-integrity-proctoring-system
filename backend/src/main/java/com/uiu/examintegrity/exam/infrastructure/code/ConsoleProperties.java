package com.uiu.examintegrity.exam.infrastructure.code;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Caps for the interactive console.
 *
 * <p>These carry more weight than the batch limits. A graded run is defensible largely because
 * nothing survives two seconds; a console keeps a program alive for minutes, so these are the
 * only thing bounding what it can cost the server.
 */
@Component
@ConfigurationProperties(prefix = "app.code.console")
public class ConsoleProperties {
    /** Killed after this long with no input written and no output produced. */
    private long idleTimeoutMs = 60_000;

    /** Killed after this long no matter how busy it is. */
    private long maxTotalMs = 300_000;

    /** Killed once it has produced this much, so a printf loop cannot stream forever. */
    private int maxOutputBytes = 256 * 1024;

    /** Across all students. One live program each is already generous. */
    private int maxConcurrent = 8;

    /**
     * How long launching waits for the browser's subscription before starting anyway. Spring's
     * simple broker neither replays nor acknowledges subscriptions, so this is what stops a quick
     * program printing its first prompt into the void.
     */
    private long subscriberWaitMs = 3_000;

    /** Longest single line a student can send to a program. */
    private int maxInputChars = 4_000;

    public long getIdleTimeoutMs() {
        return idleTimeoutMs;
    }

    public void setIdleTimeoutMs(long idleTimeoutMs) {
        this.idleTimeoutMs = idleTimeoutMs;
    }

    public long getMaxTotalMs() {
        return maxTotalMs;
    }

    public void setMaxTotalMs(long maxTotalMs) {
        this.maxTotalMs = maxTotalMs;
    }

    public int getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(int maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public long getSubscriberWaitMs() {
        return subscriberWaitMs;
    }

    public void setSubscriberWaitMs(long subscriberWaitMs) {
        this.subscriberWaitMs = subscriberWaitMs;
    }

    public int getMaxInputChars() {
        return maxInputChars;
    }

    public void setMaxInputChars(int maxInputChars) {
        this.maxInputChars = maxInputChars;
    }
}
