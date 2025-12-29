package com.onelpro.librelog.exceptions;

import java.util.List;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors != null ? List.copyOf(errors) : List.of(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of(message);
    }

    public List<String> getErrors() {
        return errors;
    }
}

