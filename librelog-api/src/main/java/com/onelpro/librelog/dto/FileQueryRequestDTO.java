package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for file query request to LibreTime.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileQueryRequestDTO {

	private AssetType assetType;
	private String contentType;
	private String musicCategory;
	private String showSegmentName;
	private Integer minDurationSeconds;
	private Integer maxDurationSeconds;
	private String fileNamePattern; // For partial name matching
	private Integer page;
	private Integer size;
	private String sortBy; // Field to sort by
	private String sortDirection; // ASC or DESC

}

