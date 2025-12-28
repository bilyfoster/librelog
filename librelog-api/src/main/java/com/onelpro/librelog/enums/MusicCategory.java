package com.onelpro.librelog.enums;

/**
 * Represents WideOrbit-compatible music categories for rotation management.
 * These categories determine how frequently songs play in the rotation.
 */
public enum MusicCategory {
	/**
	 * S1 (Power/Current): The hottest new tracks.
	 * Plays every 3-4 hours. Highest rotation priority.
	 */
	S1,

	/**
	 * S2 (Secondary): New tracks that aren't "hits" yet.
	 * Plays every 6-8 hours. Medium rotation priority.
	 */
	S2,

	/**
	 * S3 (New/Discovery): Brand new uploads being tested.
	 * Lowest rotation priority. Used for testing new music.
	 */
	S3
}

