package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestStatusTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		TestStatus[] values = TestStatus.values();
		assertEquals(4, values.length, "TestStatus should have exactly 4 values");
		assertTrue(values[0] == TestStatus.PASSED);
		assertTrue(values[1] == TestStatus.FAILED);
		assertTrue(values[2] == TestStatus.ERROR);
		assertTrue(values[3] == TestStatus.SKIPPED);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(TestStatus.PASSED, TestStatus.valueOf("PASSED"));
		assertEquals(TestStatus.FAILED, TestStatus.valueOf("FAILED"));
		assertEquals(TestStatus.ERROR, TestStatus.valueOf("ERROR"));
		assertEquals(TestStatus.SKIPPED, TestStatus.valueOf("SKIPPED"));
	}

}

