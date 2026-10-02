package com.uiu.examintegrity.exam.application.view;

/** What the editor gets back when it starts a console: the id of the topic to subscribe to. */
public record ConsoleStarted(String runId, String compileOutput) {
}
