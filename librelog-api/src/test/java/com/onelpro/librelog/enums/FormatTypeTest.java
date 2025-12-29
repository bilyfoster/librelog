package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		FormatType[] values = FormatType.values();
		assertEquals(3, values.length, "FormatType should have exactly 3 values");
		assertTrue(values[0] == FormatType.LINEAR);
		assertTrue(values[1] == FormatType.DIGITAL);
		assertTrue(values[2] == FormatType.PODCAST);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(FormatType.LINEAR, FormatType.valueOf("LINEAR"));
		assertEquals(FormatType.DIGITAL, FormatType.valueOf("DIGITAL"));
		assertEquals(FormatType.PODCAST, FormatType.valueOf("PODCAST"));
	}
}

