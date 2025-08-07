package com.bluestaq.elevatorchallenge.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Handle specific exceptions and translate them to a relevant HTTP status code
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDetails handleException(IllegalArgumentException ex, WebRequest request) {
        return new ErrorMessageDetails(ex.getMessage(), request.getDescription(false));
    }

    // Handle the case if a user requests a character floor number
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDetails handleTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return new ErrorMessageDetails("Invalid floor number format. needs to be a number 1-9", request.getDescription(false));
    }

    //handler for emergency custom exception
    @ExceptionHandler(ElevatorEmergencyException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorMessageDetails handleEmergencyException(ElevatorEmergencyException ex, WebRequest request) {
        log.error("Emergency stop blocked request: {}", request.getDescription(false));
        return new ErrorMessageDetails(ex.getMessage(), request.getDescription(false));
    }

    // Error message details class
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
