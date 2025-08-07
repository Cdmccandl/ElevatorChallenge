package com.bluestaq.elevatorChallenge.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle specific exceptions
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDetails handleException(IllegalArgumentException ex, WebRequest request) {
        return new ErrorMessageDetails(ex.getMessage(), request.getDescription(false));
    }

    // Error details class
    @Setter
    @Getter
    public static class ErrorMessageDetails {
        // Getters and setters
        private String message;
        private String details;

        public ErrorMessageDetails(String message, String details) {
            this.message = message;
            this.details = details;
        }
    }
}
