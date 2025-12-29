package com.onelpro.librelog.utils;

import com.onelpro.librelog.enums.ActionType;
import com.onelpro.librelog.enums.ModuleType;
import com.onelpro.librelog.services.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility class with static methods for common permission checks.
 * Provides convenient methods for permission validation.
 */
@Component
public class PermissionChecker {

	private static PermissionService permissionService;

	@Autowired
	public void setPermissionService(PermissionService permissionService) {
		PermissionChecker.permissionService = permissionService;
	}

	/**
	 * Checks if a user has permission to perform an action on a module at a station.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID (optional, null for tenant-level)
	 * @param moduleType the module type
	 * @param actionType the action type
	 * @return true if the user has permission, false otherwise
	 */
	public static boolean hasPermission(UUID userId, UUID stationId, ModuleType moduleType, ActionType actionType) {
		if (permissionService == null) {
			throw new IllegalStateException("PermissionService not initialized");
		}
		return permissionService.hasPermission(userId, stationId, moduleType, actionType);
	}

	/**
	 * Checks if a user can access a specific station.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID
	 * @return true if the user can access the station, false otherwise
	 */
	public static boolean canAccessStation(UUID userId, UUID stationId) {
		if (permissionService == null) {
			throw new IllegalStateException("PermissionService not initialized");
		}
		return permissionService.canAccessStation(userId, stationId);
	}

	/**
	 * Checks if a user has view permission for a module at a station.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID (optional)
	 * @param moduleType the module type
	 * @return true if the user can view, false otherwise
	 */
	public static boolean canView(UUID userId, UUID stationId, ModuleType moduleType) {
		return hasPermission(userId, stationId, moduleType, ActionType.VIEW);
	}

	/**
	 * Checks if a user has create permission for a module at a station.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID (optional)
	 * @param moduleType the module type
	 * @return true if the user can create, false otherwise
	 */
	public static boolean canCreate(UUID userId, UUID stationId, ModuleType moduleType) {
		return hasPermission(userId, stationId, moduleType, ActionType.CREATE);
	}

	/**
	 * Checks if a user has edit permission for a module at a station.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID (optional)
	 * @param moduleType the module type
	 * @return true if the user can edit, false otherwise
	 */
	public static boolean canEdit(UUID userId, UUID stationId, ModuleType moduleType) {
		return hasPermission(userId, stationId, moduleType, ActionType.EDIT);
	}

	/**
	 * Checks if a user has delete permission for a module at a station.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID (optional)
	 * @param moduleType the module type
	 * @return true if the user can delete, false otherwise
	 */
	public static boolean canDelete(UUID userId, UUID stationId, ModuleType moduleType) {
		return hasPermission(userId, stationId, moduleType, ActionType.DELETE);
	}

}

