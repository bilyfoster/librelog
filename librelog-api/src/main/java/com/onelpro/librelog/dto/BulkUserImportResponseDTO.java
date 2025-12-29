package com.onelpro.librelog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk user import response data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserImportResponseDTO {

	/**
	 * Total number of records processed from the import file.
	 */
	private Integer totalRecords;

	/**
	 * Number of users successfully imported.
	 */
	private Integer successfulCount;

	/**
	 * Number of users that failed to import.
	 */
	private Integer failedCount;

	/**
	 * List of errors encountered during import.
	 */
	private List<ImportErrorDTO> errors;

	/**
	 * List of successfully imported users.
	 */
	private List<UserResponseDTO> importedUsers;

}

