package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.FormatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for channel creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelRequestDTO {

	@NotNull(message = "Station ID is required")
	private UUID stationId;

	@NotBlank(message = "Name is required")
	private String name;

	private Integer channelNumber;

	@NotNull(message = "Format type is required")
	private FormatType formatType;

	private String description;

	@NotNull(message = "Is active flag is required")
	private Boolean isActive;

}

