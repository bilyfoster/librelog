package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PermissionLevelTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		PermissionLevel[] values = PermissionLevel.values();
		assertEquals(3, values.length, "PermissionLevel should have exactly 3 values");
		assertTrue(values[0] == PermissionLevel.FULL_ACCESS);
		assertTrue(values[1] == PermissionLevel.VIEW_ONLY);
		assertTrue(values[2] == PermissionLevel.CUSTOM);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(PermissionLevel.FULL_ACCESS, PermissionLevel.valueOf("FULL_ACCESS"));
		assertEquals(PermissionLevel.VIEW_ONLY, PermissionLevel.valueOf("VIEW_ONLY"));
		assertEquals(PermissionLevel.CUSTOM, PermissionLevel.valueOf("CUSTOM"));
	}
}

