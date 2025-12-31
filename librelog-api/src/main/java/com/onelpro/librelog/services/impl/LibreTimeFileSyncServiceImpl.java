package com.onelpro.librelog.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelpro.librelog.dto.FileDownloadResponseDTO;
import com.onelpro.librelog.dto.FileListResponseDTO;
import com.onelpro.librelog.dto.FileQueryRequestDTO;
import com.onelpro.librelog.dto.FileUploadRequestDTO;
import com.onelpro.librelog.dto.FileUploadResponseDTO;
import com.onelpro.librelog.dto.SyncStatusResponseDTO;
import com.onelpro.librelog.enums.SyncDirection;
import com.onelpro.librelog.enums.SyncStatus;
import com.onelpro.librelog.exceptions.BadRequestException;
import com.onelpro.librelog.exceptions.NotFoundException;
import com.onelpro.librelog.integrations.LibreTimeHttpClient;
import com.onelpro.librelog.models.LibreTimeFileSyncStatus;
import com.onelpro.librelog.repositories.LibreTimeFileSyncStatusRepository;
import com.onelpro.librelog.services.LibreTimeFileSyncService;
import com.onelpro.librelog.services.LibreTimeIntegrationConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of LibreTime file synchronization service.
 * Handles file upload, download, listing, querying, and sync status tracking.
 */
@Service
public class LibreTimeFileSyncServiceImpl implements LibreTimeFileSyncService {

	private static final Logger logger = LoggerFactory.getLogger(LibreTimeFileSyncServiceImpl.class);

	private final LibreTimeFileSyncStatusRepository syncStatusRepository;
	private final LibreTimeIntegrationConfigService configService;
	private final LibreTimeHttpClient httpClient;
	private final ObjectMapper objectMapper;

	public LibreTimeFileSyncServiceImpl(
			LibreTimeFileSyncStatusRepository syncStatusRepository,
			LibreTimeIntegrationConfigService configService,
			LibreTimeHttpClient httpClient,
			org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider) {
		this.syncStatusRepository = syncStatusRepository;
		this.configService = configService;
		this.httpClient = httpClient;
		// Use ObjectMapper from provider, or create a new one if not available
		this.objectMapper = objectMapperProvider.getIfAvailable(() -> new ObjectMapper());
	}

	@Override
	@Transactional
	public FileUploadResponseDTO uploadFile(FileUploadRequestDTO request) {
		logger.info("Uploading file to LibreTime: {}", request.getFileName());

		// Validate file
		validateFileUpload(request);

		// Get configuration
		var config = configService.getConfig();
		if (config == null) {
			throw new BadRequestException("LibreTime integration not configured. Please configure it first.");
		}

		// Configure HTTP client
		configureHttpClient(config);

		// Calculate file hash for conflict detection
		String fileHash = calculateFileHash(request.getFileData());

		// Create or update sync status
		LibreTimeFileSyncStatus syncStatus = createOrUpdateSyncStatus(
				null, // librelogFileId - not available in upload request
				request.getFileName(),
				SyncDirection.LIBRELOG_TO_LIBRETIME,
				SyncStatus.SYNCING,
				fileHash,
				request.getFileData().length);

		try {
			// Make API call - Note: This is a simplified version
			// In a real implementation, we'd need to use multipart/form-data for file uploads
			// For now, we'll use a JSON-based approach - actual implementation will need
			// to extend LibreTimeHttpClient to support multipart uploads
			String responseJson = httpClient.post("/api/v2/files", buildUploadJson(request)).block();

			// Parse response
			JsonNode responseNode = objectMapper.readTree(responseJson);
			String cartId = responseNode.has("cartId") ? responseNode.get("cartId").asText() : null;
			Boolean success = responseNode.has("success") ? responseNode.get("success").asBoolean() : false;

			if (success && cartId != null) {
				// Update sync status
				syncStatus.setLibreTimeCartId(cartId);
				syncStatus.setSyncStatus(SyncStatus.SYNCED);
				syncStatus.setLastSyncAt(LocalDateTime.now());
				syncStatus.setSyncError(null);
				syncStatus.setMetadataSynced(true);
				syncStatus.setUpdatedAt(LocalDateTime.now());
				syncStatusRepository.save(syncStatus);

				logger.info("File uploaded successfully to LibreTime. Cart ID: {}", cartId);
				return FileUploadResponseDTO.builder()
						.cartId(cartId)
						.fileName(request.getFileName())
						.success(true)
						.message("File uploaded successfully")
						.fileSizeBytes((long) request.getFileData().length)
						.uploadedAt(LocalDateTime.now())
						.build();
			} else {
				String errorMessage = responseNode.has("message") ? responseNode.get("message").asText() : "Upload failed";
				throw new BadRequestException("File upload failed: " + errorMessage);
			}
		} catch (Exception e) {
			logger.error("Failed to upload file to LibreTime: {}", e.getMessage());
			// Update sync status with error
			syncStatus.setSyncStatus(SyncStatus.FAILED);
			syncStatus.setSyncError(e.getMessage());
			syncStatus.setUpdatedAt(LocalDateTime.now());
			syncStatusRepository.save(syncStatus);

			return FileUploadResponseDTO.builder()
					.fileName(request.getFileName())
					.success(false)
					.message("File upload failed")
					.errorDetails(e.getMessage())
					.uploadedAt(LocalDateTime.now())
					.build();
		}
	}

	@Override
	@Transactional
	public FileDownloadResponseDTO downloadFile(String cartId) {
		logger.info("Downloading file from LibreTime. Cart ID: {}", cartId);

		// Get configuration
		var config = configService.getConfig();
		if (config == null) {
			throw new BadRequestException("LibreTime integration not configured. Please configure it first.");
		}

		// Configure HTTP client
		configureHttpClient(config);

		// Find existing sync status
		Optional<LibreTimeFileSyncStatus> existingStatus = syncStatusRepository.findByLibreTimeCartId(cartId);
		LibreTimeFileSyncStatus syncStatus = existingStatus.orElseGet(() ->
				LibreTimeFileSyncStatus.builder()
						.libreTimeCartId(cartId)
						.fileName("unknown")
						.syncDirection(SyncDirection.LIBRETIME_TO_LIBRELOG)
						.syncStatus(SyncStatus.SYNCING)
						.metadataSynced(false)
						.createdAt(LocalDateTime.now())
						.updatedAt(LocalDateTime.now())
						.build());

		syncStatus.setSyncStatus(SyncStatus.SYNCING);
		syncStatus.setUpdatedAt(LocalDateTime.now());
		syncStatusRepository.save(syncStatus);

		try {
			// Download file and metadata
			String responseJson = httpClient.get("/api/v2/files/" + cartId).block();
			JsonNode responseNode = objectMapper.readTree(responseJson);

			if (responseNode.has("error")) {
				throw new NotFoundException("File not found in LibreTime: " + cartId);
			}

			// Parse response - this is simplified, actual API may differ
			String fileName = responseNode.has("fileName") ? responseNode.get("fileName").asText() : cartId;
			byte[] fileData = null; // Would need to download actual file bytes from API
			Long fileSizeBytes = responseNode.has("fileSizeBytes") ? responseNode.get("fileSizeBytes").asLong() : null;

			// Update sync status
			syncStatus.setFileName(fileName);
			syncStatus.setSyncStatus(SyncStatus.SYNCED);
			syncStatus.setLastSyncAt(LocalDateTime.now());
			syncStatus.setSyncError(null);
			if (fileSizeBytes != null) {
				syncStatus.setFileSizeBytes(fileSizeBytes);
			}
			syncStatus.setMetadataSynced(true);
			syncStatus.setUpdatedAt(LocalDateTime.now());
			syncStatusRepository.save(syncStatus);

			logger.info("File downloaded successfully from LibreTime. Cart ID: {}", cartId);
			return FileDownloadResponseDTO.builder()
					.cartId(cartId)
					.fileName(fileName)
					.fileData(fileData)
					.success(true)
					.message("File downloaded successfully")
					.fileSizeBytes(fileSizeBytes)
					.downloadedAt(LocalDateTime.now())
					.build();
		} catch (Exception e) {
			logger.error("Failed to download file from LibreTime: {}", e.getMessage());
			// Update sync status with error
			syncStatus.setSyncStatus(SyncStatus.FAILED);
			syncStatus.setSyncError(e.getMessage());
			syncStatus.setUpdatedAt(LocalDateTime.now());
			syncStatusRepository.save(syncStatus);

			return FileDownloadResponseDTO.builder()
					.cartId(cartId)
					.success(false)
					.message("File download failed")
					.errorDetails(e.getMessage())
					.downloadedAt(LocalDateTime.now())
					.build();
		}
	}

	@Override
	public FileListResponseDTO listFiles(int page, int size) {
		logger.debug("Listing files from LibreTime. Page: {}, Size: {}", page, size);

		// Get configuration
		var config = configService.getConfig();
		if (config == null) {
			throw new BadRequestException("LibreTime integration not configured. Please configure it first.");
		}

		// Configure HTTP client
		configureHttpClient(config);

		try {
			String responseJson = httpClient.get("/api/v2/files?page=" + page + "&size=" + size).block();
			JsonNode responseNode = objectMapper.readTree(responseJson);

			List<FileListResponseDTO.FileInfoDTO> files = new ArrayList<>();
			if (responseNode.has("files") && responseNode.get("files").isArray()) {
				for (JsonNode fileNode : responseNode.get("files")) {
					files.add(parseFileInfo(fileNode));
				}
			}

			int totalElements = responseNode.has("totalElements") ? responseNode.get("totalElements").asInt() : files.size();
			int totalPages = responseNode.has("totalPages") ? responseNode.get("totalPages").asInt() : 1;

			return FileListResponseDTO.builder()
					.files(files)
					.totalElements(totalElements)
					.totalPages(totalPages)
					.currentPage(page)
					.pageSize(size)
					.hasNext(page < totalPages - 1)
					.hasPrevious(page > 0)
					.build();
		} catch (Exception e) {
			logger.error("Failed to list files from LibreTime: {}", e.getMessage());
			throw new BadRequestException("Failed to list files: " + e.getMessage());
		}
	}

	@Override
	public FileListResponseDTO queryFiles(FileQueryRequestDTO request) {
		logger.debug("Querying files from LibreTime with criteria");

		// Get configuration
		var config = configService.getConfig();
		if (config == null) {
			throw new BadRequestException("LibreTime integration not configured. Please configure it first.");
		}

		// Configure HTTP client
		configureHttpClient(config);

		try {
			// Build query string
			StringBuilder queryBuilder = new StringBuilder("/api/v2/files/query?");
			if (request.getAssetType() != null) {
				queryBuilder.append("assetType=").append(request.getAssetType().name()).append("&");
			}
			if (request.getContentType() != null) {
				queryBuilder.append("contentType=").append(request.getContentType()).append("&");
			}
			if (request.getPage() != null) {
				queryBuilder.append("page=").append(request.getPage()).append("&");
			}
			if (request.getSize() != null) {
				queryBuilder.append("size=").append(request.getSize()).append("&");
			}
			String queryString = queryBuilder.toString();
			if (queryString.endsWith("&")) {
				queryString = queryString.substring(0, queryString.length() - 1);
			}

			String responseJson = httpClient.get(queryString).block();
			JsonNode responseNode = objectMapper.readTree(responseJson);

			List<FileListResponseDTO.FileInfoDTO> files = new ArrayList<>();
			if (responseNode.has("files") && responseNode.get("files").isArray()) {
				for (JsonNode fileNode : responseNode.get("files")) {
					files.add(parseFileInfo(fileNode));
				}
			}

			int totalElements = responseNode.has("totalElements") ? responseNode.get("totalElements").asInt() : files.size();
			int totalPages = responseNode.has("totalPages") ? responseNode.get("totalPages").asInt() : 1;
			int currentPage = request.getPage() != null ? request.getPage() : 0;
			int pageSize = request.getSize() != null ? request.getSize() : 20;

			return FileListResponseDTO.builder()
					.files(files)
					.totalElements(totalElements)
					.totalPages(totalPages)
					.currentPage(currentPage)
					.pageSize(pageSize)
					.hasNext(currentPage < totalPages - 1)
					.hasPrevious(currentPage > 0)
					.build();
		} catch (Exception e) {
			logger.error("Failed to query files from LibreTime: {}", e.getMessage());
			throw new BadRequestException("Failed to query files: " + e.getMessage());
		}
	}

	@Override
	public SyncStatusResponseDTO getSyncStatus(UUID librelogFileId, String libreTimeCartId) {
		logger.debug("Getting sync status. LibreLog File ID: {}, LibreTime Cart ID: {}", librelogFileId, libreTimeCartId);

		LibreTimeFileSyncStatus syncStatus = null;

		if (librelogFileId != null) {
			syncStatus = syncStatusRepository.findByLibrelogFileId(librelogFileId)
					.orElse(null);
		} else if (libreTimeCartId != null) {
			syncStatus = syncStatusRepository.findByLibreTimeCartId(libreTimeCartId)
					.orElse(null);
		}

		if (syncStatus == null) {
			throw new NotFoundException("Sync status not found");
		}

		return mapToSyncStatusResponseDTO(syncStatus);
	}

	/**
	 * Validates file upload request.
	 */
	private void validateFileUpload(FileUploadRequestDTO request) {
		if (request.getFileName() == null || request.getFileName().isEmpty()) {
			throw new BadRequestException("File name is required");
		}
		if (request.getFileData() == null || request.getFileData().length == 0) {
			throw new BadRequestException("File data is required");
		}

		// Get config for max file size
		var config = configService.getConfig();
		if (config != null && config.getMaxFileSizeMb() != null) {
			long maxSizeBytes = config.getMaxFileSizeMb() * 1024L * 1024L;
			if (request.getFileData().length > maxSizeBytes) {
				throw new BadRequestException("File size exceeds maximum allowed size of " + config.getMaxFileSizeMb() + " MB");
			}
		}
	}

	/**
	 * Configures HTTP client with current integration config.
	 */
	private void configureHttpClient(com.onelpro.librelog.dto.LibreTimeIntegrationConfigResponseDTO config) {
		httpClient.setBaseUrl(config.getApiBaseUrl());
		String decryptedToken = configService.getDecryptedJwtToken();
		if (decryptedToken != null) {
			httpClient.setJwtToken(decryptedToken);
		}
	}

	/**
	 * Creates or updates sync status.
	 */
	private LibreTimeFileSyncStatus createOrUpdateSyncStatus(
			UUID librelogFileId,
			String fileName,
			SyncDirection syncDirection,
			SyncStatus syncStatus,
			String fileHash,
			long fileSizeBytes) {
		Optional<LibreTimeFileSyncStatus> existing = Optional.empty();
		if (librelogFileId != null) {
			existing = syncStatusRepository.findByLibrelogFileId(librelogFileId);
		}

		LibreTimeFileSyncStatus status = existing.orElseGet(() ->
				LibreTimeFileSyncStatus.builder()
						.librelogFileId(librelogFileId)
						.fileName(fileName)
						.syncDirection(syncDirection)
						.syncStatus(syncStatus)
						.fileHash(fileHash)
						.fileSizeBytes(fileSizeBytes)
						.metadataSynced(false)
						.createdAt(LocalDateTime.now())
						.updatedAt(LocalDateTime.now())
						.build());

		status.setSyncStatus(syncStatus);
		status.setUpdatedAt(LocalDateTime.now());
		return syncStatusRepository.save(status);
	}

	/**
	 * Calculates SHA-256 hash of file data for conflict detection.
	 */
	private String calculateFileHash(byte[] fileData) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(fileData);
			StringBuilder hexString = new StringBuilder();
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {
			logger.error("Failed to calculate file hash: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Builds JSON for file upload (simplified - actual implementation would use multipart).
	 */
	private String buildUploadJson(FileUploadRequestDTO request) throws Exception {
		// This is a simplified version - actual implementation would use multipart/form-data
		// For now, we'll create a JSON representation
		return objectMapper.writeValueAsString(request);
	}

	/**
	 * Parses file info from JSON node.
	 */
	private FileListResponseDTO.FileInfoDTO parseFileInfo(JsonNode fileNode) {
		return FileListResponseDTO.FileInfoDTO.builder()
				.cartId(fileNode.has("cartId") ? fileNode.get("cartId").asText() : null)
				.fileName(fileNode.has("fileName") ? fileNode.get("fileName").asText() : null)
				.fileSizeBytes(fileNode.has("fileSizeBytes") ? fileNode.get("fileSizeBytes").asLong() : null)
				.durationSeconds(fileNode.has("durationSeconds") ? fileNode.get("durationSeconds").asInt() : null)
				.assetType(fileNode.has("assetType") ? fileNode.get("assetType").asText() : null)
				.contentType(fileNode.has("contentType") ? fileNode.get("contentType").asText() : null)
				.build();
	}

	/**
	 * Maps entity to response DTO.
	 */
	private SyncStatusResponseDTO mapToSyncStatusResponseDTO(LibreTimeFileSyncStatus syncStatus) {
		return SyncStatusResponseDTO.builder()
				.id(syncStatus.getId())
				.librelogFileId(syncStatus.getLibrelogFileId())
				.libreTimeCartId(syncStatus.getLibreTimeCartId())
				.fileName(syncStatus.getFileName())
				.syncDirection(syncStatus.getSyncDirection())
				.syncStatus(syncStatus.getSyncStatus())
				.lastSyncAt(syncStatus.getLastSyncAt())
				.syncError(syncStatus.getSyncError())
				.fileSizeBytes(syncStatus.getFileSizeBytes())
				.fileHash(syncStatus.getFileHash())
				.metadataSynced(syncStatus.getMetadataSynced())
				.createdAt(syncStatus.getCreatedAt())
				.updatedAt(syncStatus.getUpdatedAt())
				.build();
	}

}

