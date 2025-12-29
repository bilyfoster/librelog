package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRoleTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		UserRole[] values = UserRole.values();
		assertEquals(6, values.length, "UserRole should have exactly 6 values");
		assertTrue(values[0] == UserRole.ADMIN);
		assertTrue(values[1] == UserRole.TRAFFIC_MANAGER);
		assertTrue(values[2] == UserRole.SALES_REP);
		assertTrue(values[3] == UserRole.PROGRAMMING);
		assertTrue(values[4] == UserRole.ACCOUNTING);
		assertTrue(values[5] == UserRole.OPERATIONS);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
		assertEquals(UserRole.TRAFFIC_MANAGER, UserRole.valueOf("TRAFFIC_MANAGER"));
		assertEquals(UserRole.SALES_REP, UserRole.valueOf("SALES_REP"));
		assertEquals(UserRole.PROGRAMMING, UserRole.valueOf("PROGRAMMING"));
		assertEquals(UserRole.ACCOUNTING, UserRole.valueOf("ACCOUNTING"));
		assertEquals(UserRole.OPERATIONS, UserRole.valueOf("OPERATIONS"));
	}

}

