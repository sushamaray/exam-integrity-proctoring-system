package com.uiu.examintegrity.exam.application.view;

import java.time.Instant;

public record SubmitResult(Long sessionId, String state, Instant submittedAt) {
}
