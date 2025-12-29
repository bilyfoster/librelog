package com.onelpro.librelog.services;

import com.onelpro.librelog.enums.ActionType;
import com.onelpro.librelog.enums.ModuleType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for permission management and checking operations.
 * Handles property-based access control, role-based permissions, and custom permissions.
 */
public interface PermissionService {

	/**
	 * Checks if a user has permission to perform a specific action on a module at a station.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID (optional, null for tenant-level permissions)
	 * @param moduleType the module type
	 * @param actionType the action type
	 * @return true if the user has permission, false otherwise
	 */
	boolean hasPermission(UUID userId, UUID stationId, ModuleType moduleType, ActionType actionType);

	/**
	 * Gets all stations a user has access to.
	 *
	 * @param userId the user ID
	 * @return list of station IDs the user can access
	 */
	List<UUID> getUserStations(UUID userId);

	/**
	 * Checks if a user can access a specific station.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID
	 * @return true if the user can access the station, false otherwise
	 */
	boolean canAccessStation(UUID userId, UUID stationId);

	/**
	 * Gets the effective permissions for a user at a station.
	 * Considers role permissions, custom role permissions, and station-specific overrides.
	 *
	 * @param userId the user ID
	 * @param stationId the station ID (optional, null for tenant-level permissions)
	 * @return map of module types to sets of allowed action types
	 */
	Map<ModuleType, List<ActionType>> getEffectivePermissions(UUID userId, UUID stationId);

	/**
	 * Evicts all permission-related caches.
	 * Should be called when user permissions or assignments change.
	 */
	void evictAllPermissionCaches();

}

