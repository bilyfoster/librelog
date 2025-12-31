package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for file upload response from LibreTime.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDTO {

	private String cartId;
	private String fileName;
	private Boolean success;
	private String message;
	private Long fileSizeBytes;
	private LocalDateTime uploadedAt;
	private String errorDetails; // Only populated if upload failed

}

