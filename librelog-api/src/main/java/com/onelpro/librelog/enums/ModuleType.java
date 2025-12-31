package com.onelpro.librelog.enums;

/**
 * Represents the modules in the broadcast ERP system that can have permission controls.
 * Each module represents a major functional area of the application.
 */
public enum ModuleType {
	/**
	 * Order Entry and Order Management module.
	 */
	ORDERS,
	
	/**
	 * Log Editing, Log Viewing, and Log Finalization module.
	 */
	LOGS,
	
	/**
	 * Avail Management and Rate Card Management module.
	 */
	INVENTORY,
	
	/**
	 * Invoicing, Accounts Receivable Management, and Reconciliation module.
	 */
	BILLING,
	
	/**
	 * Report Generation and Report Viewing module.
	 */
	REPORTS,
	
	/**
	 * Material Instructions module for associating commercial audio/video files with spots.
	 */
	MATERIAL_INSTRUCTIONS,
	
	/**
	 * Clock Template Creation and Editing module.
	 */
	CLOCK_TEMPLATES,
	
	/**
	 * User Creation, User Editing, and Permission Management module.
	 */
	USER_MANAGEMENT,
	
	/**
	 * Global Configuration and Rate Card Templates module.
	 */
	SYSTEM_SETTINGS,
	
	/**
	 * LibreTime Integration module for API integration, file synchronization, and clock export.
	 */
	LIBRETIME_INTEGRATION
}

