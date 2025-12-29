package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AuditActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for audit log response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDTO {

	private UUID id;
	private UUID userId;
	private String userEmail;
	private UUID impersonatedUserId;
	private String impersonatedUserEmail;
	private AuditActionType actionType;
	private String resourceType;
	private UUID resourceId;
	private Map<String, Object> previousValue;
	private Map<String, Object> newValue;
	private String ipAddress;
	private String userAgent;
	private UUID stationId;
	private String stationName;
	private LocalDateTime timestamp;

}

