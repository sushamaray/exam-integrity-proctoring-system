package com.uiu.examintegrity.exam.domain;

import java.time.Instant;

public class SessionExpired extends RuntimeException {
    public SessionExpired(Instant expiresAt) {
        super("Time is up -- this session closed at " + expiresAt);
    }
}
