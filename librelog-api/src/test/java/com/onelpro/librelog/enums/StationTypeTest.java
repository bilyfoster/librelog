package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		StationType[] values = StationType.values();
		assertEquals(5, values.length, "StationType should have exactly 5 values");
		assertTrue(values[0] == StationType.AM);
		assertTrue(values[1] == StationType.FM);
		assertTrue(values[2] == StationType.HD_RADIO);
		assertTrue(values[3] == StationType.DIGITAL);
		assertTrue(values[4] == StationType.SATELLITE);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(StationType.AM, StationType.valueOf("AM"));
		assertEquals(StationType.FM, StationType.valueOf("FM"));
		assertEquals(StationType.HD_RADIO, StationType.valueOf("HD_RADIO"));
		assertEquals(StationType.DIGITAL, StationType.valueOf("DIGITAL"));
		assertEquals(StationType.SATELLITE, StationType.valueOf("SATELLITE"));
	}
}

