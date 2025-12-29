package com.onelpro.librelog.exceptions;

/**
 * Exception thrown when a user attempts to perform an action they are not authorized to perform.
 */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}

	public ForbiddenException(String message, Throwable cause) {
		super(message, cause);
	}

}

