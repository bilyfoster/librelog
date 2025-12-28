package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AssetType;
import com.onelpro.librelog.enums.TimingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for fixed asset response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetResponseDTO {
	private UUID id;
	private UUID clockTemplateId;
	private String clockTemplateName;
	private String name;
	private AssetType assetType;
	private LocalTime startTime;
	private String assetIdentifier;
	private TimingType timingType;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

