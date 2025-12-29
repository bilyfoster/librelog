package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.UserResponseDTO;

import java.util.UUID;

/**
 * Service interface for user impersonation operations.
 * Allows administrators to "View as User" for troubleshooting.
 */
public interface ImpersonationService {

	/**
	 * Starts impersonating a target user.
	 *
	 * @param adminUserId the administrator user ID
	 * @param targetUserId the target user ID to impersonate
	 * @return the impersonation session token
	 */
	String startImpersonation(UUID adminUserId, UUID targetUserId);

	/**
	 * Stops impersonating a user.
	 *
	 * @param adminUserId the administrator user ID
	 */
	void stopImpersonation(UUID adminUserId);

	/**
	 * Checks if a user is currently being impersonated.
	 *
	 * @param userId the user ID
	 * @return true if the user is being impersonated, false otherwise
	 */
	boolean isImpersonating(UUID userId);

	/**
	 * Gets the user ID of the administrator who is impersonating.
	 *
	 * @param userId the user ID being impersonated
	 * @return the administrator user ID, or null if not being impersonated
	 */
	UUID getImpersonatingAdmin(UUID userId);

	/**
	 * Gets the target user being impersonated by an administrator.
	 *
	 * @param adminUserId the administrator user ID
	 * @return the target user, or null if not impersonating
	 */
	UserResponseDTO getImpersonatedUser(UUID adminUserId);

}

