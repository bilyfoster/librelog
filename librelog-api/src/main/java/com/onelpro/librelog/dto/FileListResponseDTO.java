package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for file list response from LibreTime.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileListResponseDTO {

	private List<FileInfoDTO> files;
	private Integer totalElements;
	private Integer totalPages;
	private Integer currentPage;
	private Integer pageSize;
	private Boolean hasNext;
	private Boolean hasPrevious;

	/**
	 * DTO for file information in list.
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class FileInfoDTO {
		private String cartId;
		private String fileName;
		private Long fileSizeBytes;
		private Integer durationSeconds;
		private String assetType;
		private String contentType;
	}

}

