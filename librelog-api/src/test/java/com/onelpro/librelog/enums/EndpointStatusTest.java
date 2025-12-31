package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndpointStatusTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		EndpointStatus[] values = EndpointStatus.values();
		assertEquals(4, values.length, "EndpointStatus should have exactly 4 values");
		assertTrue(values[0] == EndpointStatus.WORKING);
		assertTrue(values[1] == EndpointStatus.BROKEN);
		assertTrue(values[2] == EndpointStatus.MISSING);
		assertTrue(values[3] == EndpointStatus.UNKNOWN);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(EndpointStatus.WORKING, EndpointStatus.valueOf("WORKING"));
		assertEquals(EndpointStatus.BROKEN, EndpointStatus.valueOf("BROKEN"));
		assertEquals(EndpointStatus.MISSING, EndpointStatus.valueOf("MISSING"));
		assertEquals(EndpointStatus.UNKNOWN, EndpointStatus.valueOf("UNKNOWN"));
	}

}

