package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStatusTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		UserStatus[] values = UserStatus.values();
		assertEquals(3, values.length, "UserStatus should have exactly 3 values");
		assertTrue(values[0] == UserStatus.ACTIVE);
		assertTrue(values[1] == UserStatus.INACTIVE);
		assertTrue(values[2] == UserStatus.BANNED);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(UserStatus.ACTIVE, UserStatus.valueOf("ACTIVE"));
		assertEquals(UserStatus.INACTIVE, UserStatus.valueOf("INACTIVE"));
		assertEquals(UserStatus.BANNED, UserStatus.valueOf("BANNED"));
	}

}

