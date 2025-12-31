package com.onelpro.librelog.utils;

import com.onelpro.librelog.config.UserAuthenticationDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for accessing security context information.
 * Provides helper methods to extract user ID, station assignments, and other authentication details.
 */
public class SecurityContextUtils {

	/**
	 * Gets the current user ID from the security context.
	 *
	 * @return the user ID, or null if not authenticated
	 */
	public static UUID getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof String) {
			try {
				return UUID.fromString((String) principal);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		return null;
	}

	/**
	 * Gets the current user's station assignments from the security context.
	 *
	 * @return list of station IDs the user has access to, or empty list if not authenticated
	 */
	public static List<UUID> getCurrentUserStationIds() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Collections.emptyList();
		}

		Object details = authentication.getDetails();
		if (details instanceof UserAuthenticationDetails) {
			return ((UserAuthenticationDetails) details).getStationIds();
		}

		return Collections.emptyList();
	}

	/**
	 * Checks if the current user has access to a specific station.
	 *
	 * @param stationId the station ID to check
	 * @return true if the user has access, false otherwise
	 */
	public static boolean hasAccessToStation(UUID stationId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return false;
		}

		Object details = authentication.getDetails();
		if (details instanceof UserAuthenticationDetails) {
			return ((UserAuthenticationDetails) details).hasAccessToStation(stationId);
		}

		return false;
	}

	/**
	 * Gets the current user's email from the security context.
	 *
	 * @return the email, or null if not authenticated
	 */
	public static String getCurrentUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Object details = authentication.getDetails();
		if (details instanceof UserAuthenticationDetails) {
			return ((UserAuthenticationDetails) details).getEmail();
		}

		return null;
	}

	/**
	 * Gets the current user's role from the security context.
	 *
	 * @return the role, or null if not authenticated
	 */
	public static String getCurrentUserRole() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Object details = authentication.getDetails();
		if (details instanceof UserAuthenticationDetails) {
			return ((UserAuthenticationDetails) details).getRole();
		}

		return null;
	}

}


