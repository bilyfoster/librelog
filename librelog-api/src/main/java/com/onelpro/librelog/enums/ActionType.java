package com.onelpro.librelog.enums;

/**
 * Represents the action-level permissions that can be granted or denied for each module.
 * These actions define what operations a user can perform on resources within a module.
 */
public enum ActionType {
	/**
	 * Permission to view/read data within a module.
	 */
	VIEW,
	
	/**
	 * Permission to create new records within a module.
	 */
	CREATE,
	
	/**
	 * Permission to modify existing records within a module.
	 */
	EDIT,
	
	/**
	 * Permission to delete records within a module.
	 */
	DELETE
}

