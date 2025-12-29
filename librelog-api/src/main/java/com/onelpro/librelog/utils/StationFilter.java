package com.onelpro.librelog.utils;

import com.onelpro.librelog.services.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for filtering collections by user's station assignments.
 * Provides methods to filter entities that belong to stations the user can access.
 */
@Component
public class StationFilter {

	private static PermissionService permissionService;

	@Autowired
	public void setPermissionService(PermissionService permissionService) {
		StationFilter.permissionService = permissionService;
	}

	/**
	 * Filters a list of entities by the stations a user can access.
	 *
	 * @param <T> the entity type
	 * @param entities the list of entities to filter
	 * @param userId the user ID
	 * @param stationIdExtractor function to extract station ID from entity
	 * @return filtered list of entities
	 */
	public static <T> List<T> filterByUserStations(List<T> entities, UUID userId, Function<T, UUID> stationIdExtractor) {
		if (permissionService == null) {
			throw new IllegalStateException("PermissionService not initialized");
		}

		List<UUID> userStations = permissionService.getUserStations(userId);

		// If user has no station assignments, return empty list
		if (userStations.isEmpty()) {
			return List.of();
		}

		return entities.stream()
				.filter(entity -> {
					UUID stationId = stationIdExtractor.apply(entity);
					return stationId != null && userStations.contains(stationId);
				})
				.collect(Collectors.toList());
	}

	/**
	 * Filters a list of entities by checking if user can access each entity's station.
	 *
	 * @param <T> the entity type
	 * @param entities the list of entities to filter
	 * @param userId the user ID
	 * @param stationIdExtractor function to extract station ID from entity
	 * @return filtered list of entities
	 */
	public static <T> List<T> filterByStationAccess(List<T> entities, UUID userId, Function<T, UUID> stationIdExtractor) {
		if (permissionService == null) {
			throw new IllegalStateException("PermissionService not initialized");
		}

		return entities.stream()
				.filter(entity -> {
					UUID stationId = stationIdExtractor.apply(entity);
					return stationId != null && permissionService.canAccessStation(userId, stationId);
				})
				.collect(Collectors.toList());
	}

}

