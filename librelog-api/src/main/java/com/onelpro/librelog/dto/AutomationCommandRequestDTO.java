package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AutomationCommandType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for automation command creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationCommandRequestDTO {

	@NotNull(message = "Clock template ID is required")
	private UUID clockTemplateId;

	@NotNull(message = "Command type is required")
	private AutomationCommandType commandType;

	@NotNull(message = "Trigger time is required")
	private LocalTime triggerTime;

	private String priority;

	private Map<String, Object> parameters;

	// LibreTime integration fields
	private String libreTimePlaylistId;
	private String libreTimeSmartBlockId;
	private String libreTimeCommandType; // PLAYLIST, SMART_BLOCK, LIVE_INPUT, NETWORK_FEED, EAS_ALERT

}

