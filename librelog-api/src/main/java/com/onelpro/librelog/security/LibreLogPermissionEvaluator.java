package com.onelpro.librelog.security;

import com.onelpro.librelog.enums.ActionType;
import com.onelpro.librelog.enums.ModuleType;
import com.onelpro.librelog.services.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

/**
 * Custom permission evaluator for Spring Security.
 * Checks permissions based on user-station assignments and roles.
 */
@Component
public class LibreLogPermissionEvaluator implements PermissionEvaluator {

	private static final Logger logger = LoggerFactory.getLogger(LibreLogPermissionEvaluator.class);

	private final PermissionService permissionService;

	public LibreLogPermissionEvaluator(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return false;
		}

		// Extract user ID from authentication
		UUID userId = extractUserId(authentication);
		if (userId == null) {
			return false;
		}

		// If targetDomainObject is a UUID (station ID), check station-specific permission
		if (targetDomainObject instanceof UUID) {
			UUID stationId = (UUID) targetDomainObject;
			return checkPermission(userId, stationId, permission);
		}

		// If targetDomainObject is a string (station ID as string), parse it
		if (targetDomainObject instanceof String) {
			try {
				UUID stationId = UUID.fromString((String) targetDomainObject);
				return checkPermission(userId, stationId, permission);
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid UUID format in targetDomainObject: {}", targetDomainObject);
				return false;
			}
		}

		// For other types, return false (can be extended later)
		return false;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return false;
		}

		UUID userId = extractUserId(authentication);
		if (userId == null) {
			return false;
		}

		// If targetType is "station" and targetId is a UUID, check station permission
		if ("station".equalsIgnoreCase(targetType) && targetId instanceof UUID) {
			UUID stationId = (UUID) targetId;
			return checkPermission(userId, stationId, permission);
		}

		// If targetType is "station" and targetId is a String, parse it
		if ("station".equalsIgnoreCase(targetType) && targetId instanceof String) {
			try {
				UUID stationId = UUID.fromString((String) targetId);
				return checkPermission(userId, stationId, permission);
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid UUID format in targetId: {}", targetId);
				return false;
			}
		}

		return false;
	}

	/**
	 * Checks if a user has a specific permission for a station.
	 * Permission format: "MODULE_TYPE:ACTION_TYPE" (e.g., "ORDERS:VIEW", "LOGS:CREATE")
	 */
	private boolean checkPermission(UUID userId, UUID stationId, Object permission) {
		if (permission == null) {
			return false;
		}

		String permissionStr = permission.toString();
		if (!permissionStr.contains(":")) {
			// Simple permission check - just check if user can access the station
			if ("ACCESS".equalsIgnoreCase(permissionStr)) {
				return permissionService.canAccessStation(userId, stationId);
			}
			return false;
		}

		// Parse permission string (format: "MODULE_TYPE:ACTION_TYPE")
		String[] parts = permissionStr.split(":", 2);
		if (parts.length != 2) {
			return false;
		}

		try {
			ModuleType moduleType = ModuleType.valueOf(parts[0].toUpperCase());
			ActionType actionType = ActionType.valueOf(parts[1].toUpperCase());
			return permissionService.hasPermission(userId, stationId, moduleType, actionType);
		} catch (IllegalArgumentException e) {
			logger.warn("Invalid permission format: {}", permissionStr);
			return false;
		}
	}

	/**
	 * Extracts user ID from authentication principal.
	 */
	private UUID extractUserId(Authentication authentication) {
		Object principal = authentication.getPrincipal();
		if (principal instanceof String) {
			try {
				return UUID.fromString((String) principal);
			} catch (IllegalArgumentException e) {
				logger.warn("Invalid UUID format in authentication principal: {}", principal);
				return null;
			}
		}
		return null;
	}

}


