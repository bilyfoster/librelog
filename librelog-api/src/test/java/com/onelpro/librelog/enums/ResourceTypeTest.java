package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		ResourceType[] values = ResourceType.values();
		assertEquals(8, values.length, "ResourceType should have exactly 8 values");
		assertTrue(values[0] == ResourceType.ORDER);
		assertTrue(values[1] == ResourceType.LOG);
		assertTrue(values[2] == ResourceType.USER);
		assertTrue(values[3] == ResourceType.PERMISSION);
		assertTrue(values[4] == ResourceType.STATION);
		assertTrue(values[5] == ResourceType.ROLE);
		assertTrue(values[6] == ResourceType.CUSTOM_ROLE);
		assertTrue(values[7] == ResourceType.SESSION);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(ResourceType.ORDER, ResourceType.valueOf("ORDER"));
		assertEquals(ResourceType.LOG, ResourceType.valueOf("LOG"));
		assertEquals(ResourceType.USER, ResourceType.valueOf("USER"));
		assertEquals(ResourceType.PERMISSION, ResourceType.valueOf("PERMISSION"));
		assertEquals(ResourceType.STATION, ResourceType.valueOf("STATION"));
		assertEquals(ResourceType.ROLE, ResourceType.valueOf("ROLE"));
		assertEquals(ResourceType.CUSTOM_ROLE, ResourceType.valueOf("CUSTOM_ROLE"));
		assertEquals(ResourceType.SESSION, ResourceType.valueOf("SESSION"));
	}
}

