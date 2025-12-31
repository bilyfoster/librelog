package com.onelpro.librelog.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when LibreTime API returns an error response.
 * Contains the HTTP status code and response details.
 */
public class LibreTimeApiException extends RuntimeException {

	private final HttpStatus statusCode;
	private final String responseBody;

	public LibreTimeApiException(String message, HttpStatus statusCode) {
		super(message);
		this.statusCode = statusCode;
		this.responseBody = null;
	}

	public LibreTimeApiException(String message, HttpStatus statusCode, String responseBody) {
		super(message);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public LibreTimeApiException(String message, HttpStatus statusCode, String responseBody, Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public HttpStatus getStatusCode() {
		return statusCode;
	}

	public String getResponseBody() {
		return responseBody;
	}

}

