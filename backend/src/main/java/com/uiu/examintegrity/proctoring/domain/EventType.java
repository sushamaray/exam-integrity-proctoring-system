package com.uiu.examintegrity.proctoring.domain;

public enum EventType {
    PASTE,
    COPY,
    TAB_HIDDEN,
    TAB_VISIBLE,
    WINDOW_BLUR,
    WINDOW_FOCUS,
    FS_ENTER,
    FS_EXIT,
    HEARTBEAT,

    ANSWER;

    public boolean isLeave() {
        return this == TAB_HIDDEN || this == WINDOW_BLUR;
    }

    public boolean isReturn() {
        return this == TAB_VISIBLE || this == WINDOW_FOCUS;
    }
}
