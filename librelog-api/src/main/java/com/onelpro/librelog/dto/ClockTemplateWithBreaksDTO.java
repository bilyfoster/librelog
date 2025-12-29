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
 * DTO containing a clock template with all its associated elements:
 * breaks, fixed assets, and automation commands.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClockTemplateWithBreaksDTO {

	private UUID id;
	private String name;
	private String description;
	private UUID channelId;
	private String channelName;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@Builder.Default
	private List<BreakStructureResponseDTO> breaks = new ArrayList<>();

	@Builder.Default
	private List<FixedAssetResponseDTO> fixedAssets = new ArrayList<>();

	@Builder.Default
	private List<AutomationCommandResponseDTO> automationCommands = new ArrayList<>();

}

