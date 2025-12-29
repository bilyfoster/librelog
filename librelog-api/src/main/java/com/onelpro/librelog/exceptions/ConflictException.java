package com.onelpro.librelog.exceptions;

/**
 * Exception thrown when a resource conflict occurs (e.g., duplicate name).
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}

