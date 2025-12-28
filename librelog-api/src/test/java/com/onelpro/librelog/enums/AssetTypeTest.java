package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		AssetType[] values = AssetType.values();
		assertEquals(6, values.length, "AssetType should have exactly 6 values");
		assertTrue(values[0] == AssetType.IM);
		assertTrue(values[1] == AssetType.ID);
		assertTrue(values[2] == AssetType.CM);
		assertTrue(values[3] == AssetType.PR);
		assertTrue(values[4] == AssetType.VT);
		assertTrue(values[5] == AssetType.SH);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(AssetType.IM, AssetType.valueOf("IM"));
		assertEquals(AssetType.ID, AssetType.valueOf("ID"));
		assertEquals(AssetType.CM, AssetType.valueOf("CM"));
		assertEquals(AssetType.PR, AssetType.valueOf("PR"));
		assertEquals(AssetType.VT, AssetType.valueOf("VT"));
		assertEquals(AssetType.SH, AssetType.valueOf("SH"));
	}

}

