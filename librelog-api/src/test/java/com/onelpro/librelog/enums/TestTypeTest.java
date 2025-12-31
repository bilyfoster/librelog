package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		TestType[] values = TestType.values();
		assertEquals(5, values.length, "TestType should have exactly 5 values");
		assertTrue(values[0] == TestType.CONNECTIVITY);
		assertTrue(values[1] == TestType.AUTHENTICATION);
		assertTrue(values[2] == TestType.CRUD);
		assertTrue(values[3] == TestType.ERROR_HANDLING);
		assertTrue(values[4] == TestType.EDGE_CASE);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(TestType.CONNECTIVITY, TestType.valueOf("CONNECTIVITY"));
		assertEquals(TestType.AUTHENTICATION, TestType.valueOf("AUTHENTICATION"));
		assertEquals(TestType.CRUD, TestType.valueOf("CRUD"));
		assertEquals(TestType.ERROR_HANDLING, TestType.valueOf("ERROR_HANDLING"));
		assertEquals(TestType.EDGE_CASE, TestType.valueOf("EDGE_CASE"));
	}

}

