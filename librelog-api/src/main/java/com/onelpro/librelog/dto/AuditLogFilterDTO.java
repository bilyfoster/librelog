package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AuditActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for filtering audit logs with pagination support.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFilterDTO {

	private UUID userId;
	private AuditActionType actionType;
	private String resourceType;
	private UUID stationId;
	private LocalDateTime startDate;
	private LocalDateTime endDate;

	/**
	 * Page number (0-indexed). Default: 0
	 */
	@Builder.Default
	private Integer page = 0;

	/**
	 * Page size. Default: 20
	 */
	@Builder.Default
	private Integer size = 20;

}

