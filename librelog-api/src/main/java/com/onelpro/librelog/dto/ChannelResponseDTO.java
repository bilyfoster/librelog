package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.FormatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for channel response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponseDTO {
	private UUID id;
	private UUID stationId;
	private String stationName;
	private String stationCallSign;
	private String name;
	private Integer channelNumber;
	private FormatType formatType;
	private String description;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

