package com.uiu.examintegrity.exam.application.view;

import java.time.Instant;

public record SavedAnswer(Long questionId, Instant savedAt, long remainingSeconds) {
}
