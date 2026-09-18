package com.uiu.examintegrity.exam.api.dto;

import com.uiu.examintegrity.exam.domain.AnswerValue;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record SaveAnswerRequest(
        @Size(max = 1, message = "An option is a single letter") String optionId,
        @Size(max = 20_000, message = "Answers are limited to 20,000 characters") String textAnswer) {
    @AssertTrue(message = "Send either an option or a text answer")
    public boolean isExactlyOneAnswer() {
        return (optionId == null) != (textAnswer == null);
    }

    public AnswerValue toValue() {
        return optionId != null ? AnswerValue.choice(optionId) : AnswerValue.text(textAnswer);
    }
}
