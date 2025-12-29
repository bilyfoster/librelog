package com.onelpro.librelog.exceptions;

/**
 * Exception thrown when a request contains invalid or malformed data.
 */
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

}

