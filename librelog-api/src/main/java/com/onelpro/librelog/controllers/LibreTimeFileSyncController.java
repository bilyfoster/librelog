package com.onelpro.librelog.controllers;

import com.onelpro.librelog.dto.FileDownloadResponseDTO;
import com.onelpro.librelog.dto.FileListResponseDTO;
import com.onelpro.librelog.dto.FileQueryRequestDTO;
import com.onelpro.librelog.dto.FileUploadRequestDTO;
import com.onelpro.librelog.dto.FileUploadResponseDTO;
import com.onelpro.librelog.dto.SyncStatusResponseDTO;
import com.onelpro.librelog.services.LibreTimeFileSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for LibreTime file synchronization operations.
 */
@RestController
@RequestMapping("/api/libretime/files")
@Tag(name = "LibreTime File Sync", description = "File synchronization endpoints for LibreTime integration")
public class LibreTimeFileSyncController {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeFileSyncController.class);

	private final LibreTimeFileSyncService fileSyncService;

	public LibreTimeFileSyncController(LibreTimeFileSyncService fileSyncService) {
		this.fileSyncService = fileSyncService;
	}

	@PostMapping("/upload")
	@Operation(
			summary = "Upload file to LibreTime",
			description = "Uploads a file to LibreTime with metadata. Accepts multipart/form-data. Returns cart ID and upload status."
	)
	@ApiResponse(responseCode = "200", description = "File uploaded successfully")
	@ApiResponse(responseCode = "400", description = "Invalid request data or file validation failed")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_SYNC')")
	public ResponseEntity<FileUploadResponseDTO> uploadFile(
			@Valid @RequestBody FileUploadRequestDTO request) {
		logger.info("POST /api/libretime/files/upload - Uploading file: {}", request.getFileName());
		FileUploadResponseDTO response = fileSyncService.uploadFile(request);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/download/{cartId}")
	@Operation(
			summary = "Download file from LibreTime",
			description = "Downloads a file from LibreTime by cart ID. Returns file data and metadata."
	)
	@ApiResponse(responseCode = "200", description = "File downloaded successfully")
	@ApiResponse(responseCode = "404", description = "File not found")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<FileDownloadResponseDTO> downloadFile(@PathVariable String cartId) {
		logger.info("GET /api/libretime/files/download/{} - Downloading file", cartId);
		FileDownloadResponseDTO response = fileSyncService.downloadFile(cartId);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	@Operation(
			summary = "List files from LibreTime",
			description = "Lists files from LibreTime with pagination support. Returns files and pagination metadata."
	)
	@ApiResponse(responseCode = "200", description = "Files retrieved successfully")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<FileListResponseDTO> listFiles(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		logger.info("GET /api/libretime/files - Listing files (page: {}, size: {})", page, size);
		FileListResponseDTO response = fileSyncService.listFiles(page, size);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/query")
	@Operation(
			summary = "Query files from LibreTime",
			description = "Queries files from LibreTime by metadata criteria. Supports filtering and sorting."
	)
	@ApiResponse(responseCode = "200", description = "Query completed successfully")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<FileListResponseDTO> queryFiles(
			@Valid @RequestBody FileQueryRequestDTO request) {
		logger.info("POST /api/libretime/files/query - Querying files with criteria");
		FileListResponseDTO response = fileSyncService.queryFiles(request);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/sync-status")
	@Operation(
			summary = "Get file sync status",
			description = "Gets the synchronization status for a file. Can query by LibreLog file ID or LibreTime cart ID."
	)
	@ApiResponse(responseCode = "200", description = "Sync status retrieved successfully")
	@ApiResponse(responseCode = "404", description = "File sync status not found")
	@PreAuthorize("hasPermission(null, 'LIBRETIME_INTEGRATION_VIEW')")
	public ResponseEntity<SyncStatusResponseDTO> getSyncStatus(
			@RequestParam(required = false) UUID librelogFileId,
			@RequestParam(required = false) String libreTimeCartId) {
		logger.info("GET /api/libretime/files/sync-status - Getting sync status (librelogFileId: {}, cartId: {})", 
				librelogFileId, libreTimeCartId);
		SyncStatusResponseDTO response = fileSyncService.getSyncStatus(librelogFileId, libreTimeCartId);
		return ResponseEntity.ok(response);
	}

}

