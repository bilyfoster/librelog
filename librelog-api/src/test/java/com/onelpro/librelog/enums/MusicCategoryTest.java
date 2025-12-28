package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MusicCategoryTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		MusicCategory[] values = MusicCategory.values();
		assertEquals(3, values.length, "MusicCategory should have exactly 3 values");
		assertTrue(values[0] == MusicCategory.S1);
		assertTrue(values[1] == MusicCategory.S2);
		assertTrue(values[2] == MusicCategory.S3);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(MusicCategory.S1, MusicCategory.valueOf("S1"));
		assertEquals(MusicCategory.S2, MusicCategory.valueOf("S2"));
		assertEquals(MusicCategory.S3, MusicCategory.valueOf("S3"));
	}

}

