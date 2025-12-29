package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for bulk user import request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserImportRequestDTO {

	/**
	 * CSV or Excel file containing user data to import.
	 */
	private MultipartFile file;

	/**
	 * If true, only validate the file without importing. Default: false
	 */
	@Builder.Default
	private Boolean validateOnly = false;

}

