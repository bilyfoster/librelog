package com.onelpro.librelog.enums;

/**
 * Represents the timing type for clock template elements.
 * Determines whether an element must start at an exact time (Hard Start)
 * or can start approximately when the previous element ends (Soft Start).
 */
public enum TimingType {
	/**
	 * Hard Start: Element must trigger at exactly the specified time.
	 * Used for fixed assets like Legal IDs, News Intros that must play at exact times.
	 */
	HARD_START,

	/**
	 * Soft Start: Element triggers when the previous element ends (approximately at a time).
	 * Used for music/content blocks that can start when the previous track ends.
	 */
	SOFT_START
}

