package com.onelpro.librelog.dto;

import com.onelpro.librelog.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for file upload request to LibreTime.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequestDTO {

	@NotBlank(message = "File name is required")
	private String fileName;

	@NotNull(message = "File data is required")
	private byte[] fileData;

	private String cartId; // Optional - LibreTime will generate if not provided

	private Integer cueInMs; // Cue in point in milliseconds

	private Integer cueOutMs; // Cue out point in milliseconds

	private Integer fadeInMs; // Fade in duration in milliseconds

	private Integer fadeOutMs; // Fade out duration in milliseconds

	private AssetType assetType; // IM, ID, CM, PR, VT, SH

	private String musicCategory; // S1, S2, S3

	private String contentType; // MUSIC, TALK, INTERVIEW, MIXED, ADVERT

	private String showSegmentName;

	private Integer durationSeconds;

}

