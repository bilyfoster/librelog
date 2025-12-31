package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for clock template export validation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClockExportValidationResultDTO {

	private UUID clockTemplateId;
	private Boolean isValid;
	private List<ValidationErrorDTO> errors;
	private List<ValidationWarningDTO> warnings;
	private Integer totalItems;
	private Integer validItems;
	private Integer invalidItems;

	public void addError(String field, String message) {
		if (errors == null) {
			errors = new ArrayList<>();
		}
		errors.add(ValidationErrorDTO.builder()
				.field(field)
				.message(message)
				.build());
	}

	public void addWarning(String field, String message) {
		if (warnings == null) {
			warnings = new ArrayList<>();
		}
		warnings.add(ValidationWarningDTO.builder()
				.field(field)
				.message(message)
				.build());
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ValidationErrorDTO {
		private String field;
		private String message;
		private String suggestion; // Optional suggestion for fixing the error
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ValidationWarningDTO {
		private String field;
		private String message;
	}

}

