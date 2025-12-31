package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncDirectionTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		SyncDirection[] values = SyncDirection.values();
		assertEquals(3, values.length, "SyncDirection should have exactly 3 values");
		assertTrue(values[0] == SyncDirection.BIDIRECTIONAL);
		assertTrue(values[1] == SyncDirection.LIBRELOG_TO_LIBRETIME);
		assertTrue(values[2] == SyncDirection.LIBRETIME_TO_LIBRELOG);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(SyncDirection.BIDIRECTIONAL, SyncDirection.valueOf("BIDIRECTIONAL"));
		assertEquals(SyncDirection.LIBRELOG_TO_LIBRETIME, SyncDirection.valueOf("LIBRELOG_TO_LIBRETIME"));
		assertEquals(SyncDirection.LIBRETIME_TO_LIBRELOG, SyncDirection.valueOf("LIBRETIME_TO_LIBRELOG"));
	}

}

