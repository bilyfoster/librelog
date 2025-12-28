package com.onelpro.librelog.enums;

/**
 * Represents transition codes for clock template elements.
 * Defines how one element transitions to the next in the broadcast.
 */
public enum TransitionCode {
	/**
	 * Segue (S): Next track starts immediately as first ends.
	 * Standard transition with no overlap.
	 */
	SEGUE,

	/**
	 * Overlap (O): Next track starts while first is fading.
	 * Used for Liners over song intros, voice-overs during music.
	 */
	OVERLAP,

	/**
	 * Hard Start (H): Forces track to play at exact time.
	 * Used for Legal IDs and other time-critical elements.
	 */
	HARD_START
}

