package com.onelpro.librelog.enums;

/**
 * Represents types of automation commands that can be triggered in clock templates.
 * These commands instruct the automation system to perform specific actions.
 */
public enum AutomationCommandType {
	/**
	 * Switch to satellite feed.
	 */
	SWITCH_TO_SATELLITE,

	/**
	 * Start recording.
	 */
	START_RECORDING,

	/**
	 * Enable live mix mode.
	 */
	ENABLE_LIVE_MIX,

	/**
	 * Disable live mix mode.
	 */
	DISABLE_LIVE_MIX,

	/**
	 * Switch to network feed.
	 */
	SWITCH_TO_NETWORK,

	/**
	 * Trigger emergency alert system.
	 */
	TRIGGER_EAS,

	/**
	 * Start streaming to online platform.
	 */
	START_STREAMING,

	/**
	 * Stop streaming to online platform.
	 */
	STOP_STREAMING
}

