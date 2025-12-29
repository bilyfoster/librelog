package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an error that occurred during bulk user import.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportErrorDTO {

	/**
	 * Row number in the import file where the error occurred (1-indexed).
	 */
	private Integer rowNumber;

	/**
	 * Email address of the user that caused the error (if available).
	 */
	private String email;

	/**
	 * Error message describing what went wrong.
	 */
	private String errorMessage;

	/**
	 * Field name that caused the error (if applicable).
	 */
	private String fieldName;

}

