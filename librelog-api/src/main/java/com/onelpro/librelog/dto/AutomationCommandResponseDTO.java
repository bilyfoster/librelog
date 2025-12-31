package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AutomationCommandType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for automation command response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationCommandResponseDTO {
	private UUID id;
	private UUID clockTemplateId;
	private String clockTemplateName;
	private AutomationCommandType commandType;
	private LocalTime triggerTime;
	private String priority;
	private Map<String, Object> parameters;
	
	// LibreTime integration fields
	private String libreTimePlaylistId;
	private String libreTimeSmartBlockId;
	private String libreTimeCommandType;
	
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

