package com.uiu.examintegrity.exam.application.view;

/**
 * One thing that happened to a running program, pushed to {@code /topic/console/{runId}}.
 *
 * @param text     the chunk of output, or the reason for an ERROR
 * @param exitCode set only on EXITED
 */
public record ConsoleEvent(Type type, String text, Integer exitCode) {
    public enum Type {
        STARTED,
        STDOUT,
        STDERR,
        EXITED,
        ERROR,
    }

    public static ConsoleEvent started() {
        return new ConsoleEvent(Type.STARTED, null, null);
    }

    public static ConsoleEvent stdout(String text) {
        return new ConsoleEvent(Type.STDOUT, text, null);
    }

    public static ConsoleEvent stderr(String text) {
        return new ConsoleEvent(Type.STDERR, text, null);
    }

    public static ConsoleEvent exited(int exitCode) {
        return new ConsoleEvent(Type.EXITED, null, exitCode);
    }

    public static ConsoleEvent error(String reason) {
        return new ConsoleEvent(Type.ERROR, reason, null);
    }
}
