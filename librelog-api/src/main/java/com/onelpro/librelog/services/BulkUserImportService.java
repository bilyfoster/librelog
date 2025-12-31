package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.BulkUserImportRequestDTO;
import com.onelpro.librelog.dto.BulkUserImportResponseDTO;

/**
 * Service interface for bulk user import operations.
 * Handles CSV/Excel file uploads for onboarding multiple users.
 */
public interface BulkUserImportService {

	/**
	 * Imports users from a CSV/Excel file.
	 *
	 * @param request the import request containing the file
	 * @return the import results with success/failure counts and errors
	 */
	BulkUserImportResponseDTO importUsers(BulkUserImportRequestDTO request);

	/**
	 * Validates a CSV/Excel file without importing.
	 *
	 * @param request the import request containing the file
	 * @return the validation results with errors found
	 */
	BulkUserImportResponseDTO validateImportFile(BulkUserImportRequestDTO request);

	/**
	 * Generates a CSV template file for bulk user import.
	 *
	 * @return the CSV template as a byte array
	 */
	byte[] generateCsvTemplate();

	/**
	 * Generates an Excel template file for bulk user import.
	 *
	 * @return the Excel template as a byte array
	 */
	byte[] generateExcelTemplate();

}

