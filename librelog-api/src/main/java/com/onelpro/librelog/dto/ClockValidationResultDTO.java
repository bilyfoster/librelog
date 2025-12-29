package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for clock template validation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClockValidationResultDTO {
	
	@Builder.Default
	private Boolean isValid = true;

	@Builder.Default
	private List<String> errors = new ArrayList<>();

	@Builder.Default
	private List<String> warnings = new ArrayList<>();

	@Builder.Default
	private List<ConflictDetailDTO> conflictDetails = new ArrayList<>();

	/**
	 * Helper method to add an error.
	 */
	public void addError(String error) {
		this.errors.add(error);
		this.isValid = false;
	}

	/**
	 * Helper method to add a warning.
	 */
	public void addWarning(String warning) {
		this.warnings.add(warning);
	}

	/**
	 * Helper method to add a conflict detail.
	 */
	public void addConflict(ConflictDetailDTO conflict) {
		this.conflictDetails.add(conflict);
		this.isValid = false;
	}

}

