package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.AuditLogFilterDTO;
import com.onelpro.librelog.dto.AuditLogResponseDTO;
import com.onelpro.librelog.enums.AuditActionType;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for audit logging operations.
 * Handles comprehensive action logging for compliance and troubleshooting.
 */
public interface AuditService {

	/**
	 * Logs a general action in the audit trail.
	 *
	 * @param actionType the type of action performed
	 * @param resourceType the type of resource affected
	 * @param resourceId the ID of the resource affected
	 * @param previousValue the previous value (before change)
	 * @param newValue the new value (after change)
	 * @param userId the user ID who performed the action
	 * @param stationId the station ID (optional)
	 * @param ipAddress the IP address of the user
	 * @param userAgent the user agent string
	 */
	void logAction(AuditActionType actionType, String resourceType, UUID resourceId,
	               Map<String, Object> previousValue, Map<String, Object> newValue,
	               UUID userId, UUID stationId, String ipAddress, String userAgent);

	/**
	 * Logs a user login event.
	 *
	 * @param userId the user ID
	 * @param ipAddress the IP address
	 * @param userAgent the user agent string
	 */
	void logLogin(UUID userId, String ipAddress, String userAgent);

	/**
	 * Logs a user logout event.
	 *
	 * @param userId the user ID
	 * @param ipAddress the IP address
	 * @param userAgent the user agent string
	 */
	void logLogout(UUID userId, String ipAddress, String userAgent);

	/**
	 * Logs a permission change event.
	 *
	 * @param userId the user ID who made the change
	 * @param targetUserId the user ID whose permissions were changed
	 * @param previousPermissions the previous permissions
	 * @param newPermissions the new permissions
	 * @param ipAddress the IP address
	 * @param userAgent the user agent string
	 */
	void logPermissionChange(UUID userId, UUID targetUserId,
	                      Map<String, Object> previousPermissions, Map<String, Object> newPermissions,
	                      String ipAddress, String userAgent);

	/**
	 * Gets audit logs with filtering and pagination.
	 *
	 * @param filter the filter criteria
	 * @return page of audit log entries
	 */
	Page<AuditLogResponseDTO> getAuditLogs(AuditLogFilterDTO filter);

	/**
	 * Exports audit logs to CSV/Excel format.
	 *
	 * @param filter the filter criteria
	 * @return byte array containing the exported file
	 */
	byte[] exportAuditLogs(AuditLogFilterDTO filter);

}

