package com.uiu.examintegrity.exam.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One line typed at a running program. Empty is allowed -- pressing Enter is a valid answer. */
public record ConsoleInputRequest(
        @NotNull(message = "There is nothing to send") @Size(max = 4_000,
                message = "That line is too long to send") String line) {
}
