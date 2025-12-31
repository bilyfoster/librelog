package com.onelpro.librelog.exceptions;

/**
 * Exception thrown when there is a connection error communicating with LibreTime API.
 * This includes network errors, timeouts, and unreachable endpoints.
 */
public class LibreTimeConnectionException extends RuntimeException {

	public LibreTimeConnectionException(String message) {
		super(message);
	}

	public LibreTimeConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

}

