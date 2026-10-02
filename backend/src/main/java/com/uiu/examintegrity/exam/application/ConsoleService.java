package com.uiu.examintegrity.exam.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.uiu.examintegrity.common.BadRequestException;
import com.uiu.examintegrity.exam.api.ConsoleBroadcaster;
import com.uiu.examintegrity.exam.api.ConsoleSubscriptions;
import com.uiu.examintegrity.exam.application.view.ConsoleEvent;
import com.uiu.examintegrity.exam.application.view.ConsoleStarted;
import com.uiu.examintegrity.exam.domain.SessionClosed;
import com.uiu.examintegrity.exam.domain.code.Language;
import com.uiu.examintegrity.exam.domain.code.SourceGuard;
import com.uiu.examintegrity.exam.infrastructure.code.ConsoleProperties;
import com.uiu.examintegrity.exam.infrastructure.code.InteractiveRunner;
import com.uiu.examintegrity.exam.infrastructure.code.Toolchains;

/**
 * Starting, feeding and stopping a program somebody is typing at.
 *
 * <p>Two ways in. Inside an exam the sitting is checked first and the run is scoped to it, so
 * submitting the exam kills it. Outside one -- the practice scratchpad -- there is no sitting, no
 * question and nothing graded, and the run is scoped to the person instead. Everything after that
 * point is identical, including the {@code SourceGuard}: a free compiler is if anything a more
 * attractive place to try something out of bounds than a graded one.
 *
 * <p>No console run is ever persisted. Only Submit produces the {@code CodeSubmission} a grade
 * comes from.
 */
@Service
public class ConsoleService {
    private final SittingGuard guard;
    private final InteractiveRunner runner;
    private final ConsoleBroadcaster broadcaster;
    private final ConsoleSubscriptions subscriptions;
    private final ConsoleProperties properties;
    private final Toolchains toolchains;

    public ConsoleService(SittingGuard guard, InteractiveRunner runner, ConsoleBroadcaster broadcaster,
            ConsoleSubscriptions subscriptions, ConsoleProperties properties, Toolchains toolchains) {
        this.guard = guard;
        this.runner = runner;
        this.broadcaster = broadcaster;
        this.subscriptions = subscriptions;
        this.properties = properties;
        this.toolchains = toolchains;
    }

    /**
     * Compiles and reserves a run against an exam question. Nothing is executing yet -- the client
     * subscribes to the returned run's topic and then calls {@link #launch}, so no output can be
     * published before anybody is listening.
     *
     * @return a run id, or a null id with the compiler's diagnostics when it did not build
     */
    public ConsoleStarted prepare(Long sessionId, Long userId, Long questionId, String languageId, String source) {
        Language language = checked(languageId, source);
        guard.codingQuestion(sessionId, userId, questionId);
        return reserve(InteractiveRunner.sessionScope(sessionId), userId, language, source);
    }

    /** The same, with no exam behind it. Open to anyone signed in, students and faculty alike. */
    public ConsoleStarted preparePractice(Long userId, String languageId, String source) {
        Language language = checked(languageId, source);
        return reserve(InteractiveRunner.practiceScope(userId), userId, language, source);
    }

    private Language checked(String languageId, String source) {
        Language language = Language.parse(languageId)
                .orElseThrow(() -> new BadRequestException("There is no language called " + languageId));
        if (!toolchains.ready(language)) {
            throw new BadRequestException(toolchains.refusal(language));
        }
        SourceGuard.reject(language, source).ifPresent(why -> {
            throw new BadRequestException("That program was not started: " + why);
        });
        return language;
    }

    private ConsoleStarted reserve(String scope, Long userId, Language language, String source) {
        try {
            return new ConsoleStarted(runner.reserve(scope, userId, language, source), null);
        } catch (InteractiveRunner.CompileFailed failed) {
            return new ConsoleStarted(null, failed.getMessage());
        }
    }

    /**
     * Starts the reserved program, once somebody is listening.
     *
     * <p>The wait is what stops a quick program printing its prompt before the browser's
     * subscription is live. It is a wait rather than a refusal: if the socket never arrives the
     * program still runs, because failing to run at all would be the worse outcome.
     */
    public void launch(String runId, Long userId) {
        subscriptions.awaitSubscriber(runId, properties.getSubscriberWaitMs());
        runner.launch(runId, userId, event -> broadcaster.send(runId, event));
    }

    public void write(String runId, Long userId, String line) {
        runner.write(runId, userId, line, event -> broadcaster.send(runId, event));
    }

    public void stop(String runId, Long userId) {
        runner.stop(runId, userId);
        broadcaster.send(runId, ConsoleEvent.error("Stopped."));
        subscriptions.forget(runId);
    }

    /**
     * A program must not outlive its exam. {@code SessionService.close} publishes this on submit
     * and on auto-submit, so the kill is immediate rather than waiting for the sweeper.
     */
    @EventListener
    public void onSessionClosed(SessionClosed closed) {
        runner.stopForScope(InteractiveRunner.sessionScope(closed.sessionId()), "the exam was submitted");
    }
}
