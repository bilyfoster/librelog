package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Report DTO describing scheduling exceptions generated while building logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingExceptionReportDTO {

	private UUID clockTemplateId;
	private LocalDate startDate;
	private LocalDate endDate;

	@Builder.Default
	private Integer totalHoursGenerated = 0;

	@Builder.Default
	private Integer totalExceptions = 0;

	@Builder.Default
	private List<SchedulingExceptionItemDTO> exceptions = new ArrayList<>();

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SchedulingExceptionItemDTO {
		private LocalDate date;
		private Integer hour;
		private String breakName;
		private String category;
		private String reason;
	}
}
