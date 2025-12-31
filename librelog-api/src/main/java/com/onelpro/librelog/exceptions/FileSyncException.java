package com.onelpro.librelog.exceptions;

/**
 * Exception thrown when file synchronization operations fail.
 * This includes upload failures, download failures, file not found errors, and permission denied errors.
 */
public class FileSyncException extends RuntimeException {

	private final String fileId;
	private final String operation;

	public FileSyncException(String message) {
		super(message);
		this.fileId = null;
		this.operation = null;
	}

	public FileSyncException(String message, Throwable cause) {
		super(message, cause);
		this.fileId = null;
		this.operation = null;
	}

	public FileSyncException(String message, String fileId, String operation) {
		super(message);
		this.fileId = fileId;
		this.operation = operation;
	}

	public FileSyncException(String message, String fileId, String operation, Throwable cause) {
		super(message, cause);
		this.fileId = fileId;
		this.operation = operation;
	}

	public String getFileId() {
		return fileId;
	}

	public String getOperation() {
		return operation;
	}

}

