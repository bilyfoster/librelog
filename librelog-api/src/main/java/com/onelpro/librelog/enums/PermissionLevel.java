package com.onelpro.librelog.enums;

/**
 * Represents the permission levels that can be assigned to users for stations, markets, or clusters.
 * Permission levels determine the scope of access a user has to resources within a specific property.
 */
public enum PermissionLevel {
	/**
	 * Full access to all operations (view, create, edit, delete) for the assigned property.
	 */
	FULL_ACCESS,
	
	/**
	 * Read-only access. User can view data but cannot create, edit, or delete.
	 */
	VIEW_ONLY,
	
	/**
	 * Custom permissions defined per module and action. Permissions are stored in JSON format
	 * and allow fine-grained control over what a user can do.
	 */
	CUSTOM
}

