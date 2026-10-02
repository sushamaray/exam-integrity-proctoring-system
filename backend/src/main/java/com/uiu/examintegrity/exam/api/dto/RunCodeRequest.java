package com.uiu.examintegrity.exam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The source to run. The cap matches {@link SaveAnswerRequest}, which stores the same text. */
public record RunCodeRequest(
        @NotBlank(message = "Choose a language") String language,
        @NotBlank(message = "There is no code to run") @Size(max = 20_000,
                message = "That is more code than an answer can hold") String sourceCode) {
}
