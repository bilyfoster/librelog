package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimingTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		TimingType[] values = TimingType.values();
		assertEquals(2, values.length, "TimingType should have exactly 2 values");
		assertTrue(values[0] == TimingType.HARD_START);
		assertTrue(values[1] == TimingType.SOFT_START);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(TimingType.HARD_START, TimingType.valueOf("HARD_START"));
		assertEquals(TimingType.SOFT_START, TimingType.valueOf("SOFT_START"));
	}

}

