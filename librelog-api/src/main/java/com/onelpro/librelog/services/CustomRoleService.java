package com.onelpro.librelog.services;

import com.onelpro.librelog.dto.CustomRoleRequestDTO;
import com.onelpro.librelog.dto.CustomRoleResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for custom role management operations.
 * Handles creation, modification, and deletion of user-defined roles.
 */
public interface CustomRoleService {

	/**
	 * Creates a new custom role.
	 *
	 * @param request the role creation request
	 * @param createdByUserId the user ID who is creating the role
	 * @return the created role
	 */
	CustomRoleResponseDTO createRole(CustomRoleRequestDTO request, UUID createdByUserId);

	/**
	 * Updates an existing custom role.
	 *
	 * @param roleId the role ID
	 * @param request the role update request
	 * @return the updated role
	 */
	CustomRoleResponseDTO updateRole(UUID roleId, CustomRoleRequestDTO request);

	/**
	 * Deletes a custom role (only if not assigned to any users).
	 *
	 * @param roleId the role ID
	 */
	void deleteRole(UUID roleId);

	/**
	 * Gets a custom role by ID.
	 *
	 * @param roleId the role ID
	 * @return the role
	 */
	CustomRoleResponseDTO getRoleById(UUID roleId);

	/**
	 * Gets all custom roles.
	 *
	 * @return list of all custom roles
	 */
	List<CustomRoleResponseDTO> getAllRoles();

	/**
	 * Clones an existing role with a new name.
	 *
	 * @param roleId the role ID to clone
	 * @param newName the new role name
	 * @param createdByUserId the user ID who is creating the clone
	 * @return the cloned role
	 */
	CustomRoleResponseDTO cloneRole(UUID roleId, String newName, UUID createdByUserId);

	/**
	 * Gets all roles assigned to a user.
	 *
	 * @param userId the user ID
	 * @return list of roles assigned to the user
	 */
	List<CustomRoleResponseDTO> getRolesAssignedToUser(UUID userId);

}

