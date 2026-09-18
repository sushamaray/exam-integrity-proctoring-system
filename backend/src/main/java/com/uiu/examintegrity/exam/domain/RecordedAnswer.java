package com.uiu.examintegrity.exam.domain;

import java.time.Instant;

public record RecordedAnswer(Long sessionId, Long questionId, AnswerValue value, Instant at) {
}
