package com.onelpro.librelog.enums;

/**
 * Represents the different formats/types of broadcast channels and inventory.
 */
public enum FormatType {
	/**
	 * Linear broadcast format (traditional radio/TV).
	 * Inventory is defined by a 24-hour clock with spots placed in specific breaks.
	 */
	LINEAR,

	/**
	 * Digital/OTT format (online streaming).
	 * Inventory is defined by impressions with dynamic ad serving.
	 */
	DIGITAL,

	/**
	 * Podcasting format.
	 * Can be a mix of "baked-in" (linear-style) and "dynamic insertion" (digital-style).
	 */
	PODCAST
}

