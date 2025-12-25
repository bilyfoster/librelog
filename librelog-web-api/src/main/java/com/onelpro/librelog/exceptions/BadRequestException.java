package com.onelpro.librelog.exceptions;

/**
 * Exception thrown when a bad request is made (e.g., invalid parameters).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

