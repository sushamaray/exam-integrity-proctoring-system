package com.uiu.examintegrity.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions thrown anywhere in a controller into an {@link ApiError}.
 * Keeping this in one place is what stops individual controllers from inventing
 * their own error formats.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Every authentication failure gets the same message on purpose. Saying
     * "no such user" versus "wrong password" tells an attacker which emails
     * are registered.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity
                .badRequest()
                .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), "Please check the highlighted fields", fieldErrors));
    }
}
