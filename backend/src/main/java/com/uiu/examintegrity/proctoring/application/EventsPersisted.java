package com.uiu.examintegrity.proctoring.application;

import java.util.List;

import com.uiu.examintegrity.proctoring.domain.ProctoringEvent;

public record EventsPersisted(Long sessionId, Long examId, List<ProctoringEvent> events) {
}
