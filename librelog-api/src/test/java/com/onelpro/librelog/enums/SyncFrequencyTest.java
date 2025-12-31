package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncFrequencyTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		SyncFrequency[] values = SyncFrequency.values();
		assertEquals(5, values.length, "SyncFrequency should have exactly 5 values");
		assertTrue(values[0] == SyncFrequency.REAL_TIME);
		assertTrue(values[1] == SyncFrequency.FIVE_MINUTES);
		assertTrue(values[2] == SyncFrequency.FIFTEEN_MINUTES);
		assertTrue(values[3] == SyncFrequency.HOURLY);
		assertTrue(values[4] == SyncFrequency.MANUAL);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(SyncFrequency.REAL_TIME, SyncFrequency.valueOf("REAL_TIME"));
		assertEquals(SyncFrequency.FIVE_MINUTES, SyncFrequency.valueOf("FIVE_MINUTES"));
		assertEquals(SyncFrequency.FIFTEEN_MINUTES, SyncFrequency.valueOf("FIFTEEN_MINUTES"));
		assertEquals(SyncFrequency.HOURLY, SyncFrequency.valueOf("HOURLY"));
		assertEquals(SyncFrequency.MANUAL, SyncFrequency.valueOf("MANUAL"));
	}

}

