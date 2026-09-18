package com.uiu.examintegrity.proctoring.api.dto;

import com.uiu.examintegrity.proctoring.application.ReviewDecision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotNull(message = "Choose a decision") ReviewDecision decision,
        @Size(max = 2000, message = "Notes are limited to 2,000 characters") String note) {
}
