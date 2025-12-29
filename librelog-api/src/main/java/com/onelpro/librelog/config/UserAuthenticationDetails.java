package com.onelpro.librelog.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;
import java.util.UUID;

/**
 * Extended authentication details that include user's station assignments.
 * This allows controllers and services to access station assignments from the security context.
 */
public class UserAuthenticationDetails extends WebAuthenticationDetails {

	private final UUID userId;
	private final String email;
	private final String role;
	private final List<UUID> stationIds;

	public UserAuthenticationDetails(UUID userId, String email, String role, List<UUID> stationIds,
	                                HttpServletRequest request) {
		super(request);
		this.userId = userId;
		this.email = email;
		this.role = role;
		this.stationIds = stationIds;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public String getRole() {
		return role;
	}

	public List<UUID> getStationIds() {
		return stationIds;
	}

	/**
	 * Checks if the user has access to a specific station.
	 */
	public boolean hasAccessToStation(UUID stationId) {
		return stationIds.contains(stationId);
	}

}

