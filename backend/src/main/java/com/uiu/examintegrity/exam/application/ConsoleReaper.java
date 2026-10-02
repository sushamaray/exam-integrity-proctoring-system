package com.uiu.examintegrity.exam.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.uiu.examintegrity.exam.api.ConsoleBroadcaster;
import com.uiu.examintegrity.exam.application.view.ConsoleEvent;
import com.uiu.examintegrity.exam.infrastructure.code.InteractiveRunner;

/**
 * Ends console runs that have outstayed their caps, following the shape of
 * {@code AutoSubmitScheduler}.
 *
 * <p>This is what catches the student who simply closes the tab. A WebSocket disconnect is not a
 * reliable signal -- a dropped connection reconnects, and a browser does not always send one --
 * so the sweep is the backstop rather than an optimisation.
 */
@Component
public class ConsoleReaper {
    private static final Logger log = LoggerFactory.getLogger(ConsoleReaper.class);

    private final InteractiveRunner runner;
    private final ConsoleBroadcaster broadcaster;

    public ConsoleReaper(InteractiveRunner runner, ConsoleBroadcaster broadcaster) {
        this.runner = runner;
        this.broadcaster = broadcaster;
    }

    @Scheduled(fixedDelayString = "${app.code.console.sweep-interval-ms:10000}")
    public void reap() {
        for (InteractiveRunner.Expired expired : runner.expired()) {
            try {
                broadcaster.send(expired.runId(),
                        ConsoleEvent.error("This program was stopped because " + expired.reason() + "."));
                runner.stop(expired.runId(), expired.reason());
            } catch (RuntimeException e) {
                log.warn("Could not reap console run {}", expired.runId(), e);
            }
        }
    }
}
