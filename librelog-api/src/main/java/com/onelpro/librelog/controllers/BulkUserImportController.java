package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.BulkUserImportRequestDTO;
import com.onelpro.librelog.dto.BulkUserImportResponseDTO;
import com.onelpro.librelog.services.BulkUserImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for bulk user import endpoints.
 */
@RestController
@RequestMapping("/api/users/bulk-import")
@Tag(name = "Bulk User Import", description = "Bulk user import from CSV/Excel files")
public class BulkUserImportController {

	private static final Logger logger = LoggerFactory.getLogger(BulkUserImportController.class);

	private final BulkUserImportService bulkUserImportService;

	public BulkUserImportController(BulkUserImportService bulkUserImportService) {
		this.bulkUserImportService = bulkUserImportService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Import users from file",
			description = "Imports users from a CSV or Excel file. Supports partial import with detailed error reporting. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Import completed (check response for success/failure counts)")
	@ApiResponse(responseCode = "400", description = "Invalid file format or file is empty")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<BulkUserImportResponseDTO> importUsers(
			@RequestParam("file") MultipartFile file,
			@RequestParam(defaultValue = "false") boolean validateOnly) {
		logger.info("Bulk user import request: fileName={}, validateOnly={}", file.getOriginalFilename(), validateOnly);

		BulkUserImportRequestDTO request = BulkUserImportRequestDTO.builder()
				.file(file)
				.validateOnly(validateOnly)
				.build();

		BulkUserImportResponseDTO response = bulkUserImportService.importUsers(request);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/template")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(
			summary = "Download import template",
			description = "Downloads a CSV template file with headers for bulk user import. Requires ADMIN role."
	)
	@ApiResponse(responseCode = "200", description = "Template file downloaded successfully")
	@ApiResponse(responseCode = "403", description = "Access denied")
	public ResponseEntity<Resource> downloadTemplate(
			@RequestParam(defaultValue = "csv") String format) {
		logger.info("Download import template request: format={}", format);

		byte[] templateBytes;
		String filename;
		MediaType contentType;

		if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
			templateBytes = bulkUserImportService.generateExcelTemplate();
			filename = "user-import-template.xlsx";
			contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		} else {
			templateBytes = bulkUserImportService.generateCsvTemplate();
			filename = "user-import-template.csv";
			contentType = MediaType.TEXT_PLAIN;
		}

		ByteArrayResource resource = new ByteArrayResource(templateBytes);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
		headers.setContentType(contentType);

		return ResponseEntity.ok()
				.headers(headers)
				.body(resource);
	}

}


