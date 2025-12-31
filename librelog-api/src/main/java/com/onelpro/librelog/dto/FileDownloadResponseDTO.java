package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for file download response from LibreTime.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadResponseDTO {

	private String cartId;
	private String fileName;
	private byte[] fileData;
	private Boolean success;
	private String message;
	private Long fileSizeBytes;
	private Integer cueInMs;
	private Integer cueOutMs;
	private Integer fadeInMs;
	private Integer fadeOutMs;
	private AssetType assetType;
	private String musicCategory;
	private String contentType;
	private String showSegmentName;
	private Integer durationSeconds;
	private LocalDateTime downloadedAt;
	private String errorDetails; // Only populated if download failed

}

