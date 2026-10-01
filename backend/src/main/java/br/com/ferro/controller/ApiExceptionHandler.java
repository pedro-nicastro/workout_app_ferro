package br.com.ferro.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> illegalArgument(IllegalArgumentException e) {
        return Map.of("error", e.getMessage() == null ? "Invalid data." : e.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> duplicate(org.springframework.dao.DuplicateKeyException e) {
        return Map.of("error", "A record with these details already exists.");
    }


    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> database(org.springframework.dao.DataAccessException e) {
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) message = "Could not access the database.";
        return Map.of("error", "Database unavailable.", "detail", message);
    }

    @ExceptionHandler(org.springframework.dao.EmptyResultDataAccessException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(org.springframework.dao.EmptyResultDataAccessException e) {
        return Map.of("error", "Record not found.");
    }
}
