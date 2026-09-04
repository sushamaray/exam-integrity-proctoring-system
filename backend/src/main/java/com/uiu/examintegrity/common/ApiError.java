package com.uiu.examintegrity.common;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape every failing endpoint returns, so the frontend only
 * ever has to know how to read one thing.
 *
 * @param fieldErrors populated for validation failures, empty otherwise
 */
public record ApiError(
        Instant timestamp,
        int status,
        String message,
        Map<String, String> fieldErrors) {

    public static ApiError of(int status, String message) {
        return new ApiError(Instant.now(), status, message, Map.of());
    }

    public static ApiError of(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, message, fieldErrors);
    }
}
