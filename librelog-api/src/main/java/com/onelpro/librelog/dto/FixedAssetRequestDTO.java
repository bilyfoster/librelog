package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AssetType;
import com.onelpro.librelog.enums.MusicCategory;
import com.onelpro.librelog.enums.TimingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for fixed asset creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetRequestDTO {

	@NotNull(message = "Clock template ID is required")
	private UUID clockTemplateId;

	@NotBlank(message = "Name is required")
	private String name;

	@NotNull(message = "Asset type is required")
	private AssetType assetType;

	@NotNull(message = "Start time is required")
	private LocalTime startTime;

	private String assetIdentifier;

	private TimingType timingType;

	private MusicCategory musicCategory;

	private String showSegmentName;

}

