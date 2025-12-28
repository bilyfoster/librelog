package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransitionCodeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		TransitionCode[] values = TransitionCode.values();
		assertEquals(3, values.length, "TransitionCode should have exactly 3 values");
		assertTrue(values[0] == TransitionCode.SEGUE);
		assertTrue(values[1] == TransitionCode.OVERLAP);
		assertTrue(values[2] == TransitionCode.HARD_START);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(TransitionCode.SEGUE, TransitionCode.valueOf("SEGUE"));
		assertEquals(TransitionCode.OVERLAP, TransitionCode.valueOf("OVERLAP"));
		assertEquals(TransitionCode.HARD_START, TransitionCode.valueOf("HARD_START"));
	}

}

