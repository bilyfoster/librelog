package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.FileDownloadResponseDTO;
import com.onelpro.librelog.dto.FileListResponseDTO;
import com.onelpro.librelog.dto.FileQueryRequestDTO;
import com.onelpro.librelog.dto.FileUploadRequestDTO;
import com.onelpro.librelog.dto.FileUploadResponseDTO;
import com.onelpro.librelog.dto.SyncStatusResponseDTO;

import java.util.UUID;

/**
 * Service interface for LibreTime file synchronization operations.
 */
public interface LibreTimeFileSyncService {

	/**
	 * Uploads a file to LibreTime.
	 * 
	 * @param request The file upload request with file data and metadata
	 * @return Upload response with cart ID and status
	 */
	FileUploadResponseDTO uploadFile(FileUploadRequestDTO request);

	/**
	 * Downloads a file from LibreTime.
	 * 
	 * @param cartId The LibreTime cart ID
	 * @return Download response with file data and metadata
	 */
	FileDownloadResponseDTO downloadFile(String cartId);

	/**
	 * Lists files from LibreTime with pagination.
	 * 
	 * @param page Page number (0-based)
	 * @param size Page size
	 * @return List response with files and pagination metadata
	 */
	FileListResponseDTO listFiles(int page, int size);

	/**
	 * Queries files from LibreTime by metadata criteria.
	 * 
	 * @param request The query request with filter criteria
	 * @return List response with matching files
	 */
	FileListResponseDTO queryFiles(FileQueryRequestDTO request);

	/**
	 * Gets the sync status for a file.
	 * 
	 * @param librelogFileId The LibreLog file ID (optional)
	 * @param libreTimeCartId The LibreTime cart ID (optional)
	 * @return Sync status response
	 */
	SyncStatusResponseDTO getSyncStatus(UUID librelogFileId, String libreTimeCartId);

}

