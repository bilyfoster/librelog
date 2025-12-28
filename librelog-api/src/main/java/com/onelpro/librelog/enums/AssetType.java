package com.onelpro.librelog.enums;

/**
 * Represents WideOrbit-compatible asset types for clock template elements.
 * These types categorize different kinds of audio content in the broadcast.
 */
public enum AssetType {
	/**
	 * IM (Imaging): Sweepers and Stingers.
	 * Short audio elements used for station branding and transitions.
	 */
	IM,

	/**
	 * ID (Legal ID): Top-of-hour legal identification.
	 * Required FCC identification announcements.
	 */
	ID,

	/**
	 * CM (Commercials/Spots): Paid advertisements.
	 * Commercial breaks and paid spots.
	 */
	CM,

	/**
	 * PR (Promos): Internal station advertisements.
	 * Promotional content for station events, shows, etc.
	 */
	PR,

	/**
	 * VT (Voice Tracks): DJ talk segments.
	 * Recorded voice tracks from DJs or hosts.
	 */
	VT,

	/**
	 * SH (Show/Longform): 5-minute interview segments and long-form content.
	 * Extended content like interviews, features, etc.
	 */
	SH
}

