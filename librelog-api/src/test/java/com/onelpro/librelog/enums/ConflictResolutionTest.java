package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConflictResolutionTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		ConflictResolution[] values = ConflictResolution.values();
		assertEquals(4, values.length, "ConflictResolution should have exactly 4 values");
		assertTrue(values[0] == ConflictResolution.LAST_WRITE_WINS);
		assertTrue(values[1] == ConflictResolution.MANUAL);
		assertTrue(values[2] == ConflictResolution.LIBRELOG_PRIORITY);
		assertTrue(values[3] == ConflictResolution.LIBRETIME_PRIORITY);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(ConflictResolution.LAST_WRITE_WINS, ConflictResolution.valueOf("LAST_WRITE_WINS"));
		assertEquals(ConflictResolution.MANUAL, ConflictResolution.valueOf("MANUAL"));
		assertEquals(ConflictResolution.LIBRELOG_PRIORITY, ConflictResolution.valueOf("LIBRELOG_PRIORITY"));
		assertEquals(ConflictResolution.LIBRETIME_PRIORITY, ConflictResolution.valueOf("LIBRETIME_PRIORITY"));
	}

}

