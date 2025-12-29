package com.onelpro.librelog.exceptions;

/**
 * Exception thrown when a resource conflict occurs (e.g., duplicate entry).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

