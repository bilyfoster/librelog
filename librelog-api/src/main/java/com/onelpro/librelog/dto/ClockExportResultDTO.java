package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for clock template export operation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClockExportResultDTO {

	private UUID clockTemplateId;
	private Boolean success;
	private String message;
	private LocalDateTime exportedAt;
	private Integer totalShowInstances;
	private Integer successfulShowInstances;
	private Integer failedShowInstances;
	private List<ExportFailureDTO> failures;
	private List<ExportWarningDTO> warnings;
	private String libreTimeResponse;

	public void addFailure(String showInstanceName, String reason) {
		if (failures == null) {
			failures = new ArrayList<>();
		}
		failures.add(ExportFailureDTO.builder()
				.showInstanceName(showInstanceName)
				.reason(reason)
				.build());
	}

	public void addWarning(String showInstanceName, String message) {
		if (warnings == null) {
			warnings = new ArrayList<>();
		}
		warnings.add(ExportWarningDTO.builder()
				.showInstanceName(showInstanceName)
				.message(message)
				.build());
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ExportFailureDTO {
		private String showInstanceName;
		private String reason;
		private String errorDetails;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ExportWarningDTO {
		private String showInstanceName;
		private String message;
	}

}

