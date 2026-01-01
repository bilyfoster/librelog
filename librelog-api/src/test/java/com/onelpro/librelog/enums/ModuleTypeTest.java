package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModuleTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		ModuleType[] values = ModuleType.values();
		assertEquals(10, values.length, "ModuleType should have exactly 10 values");
		assertTrue(values[0] == ModuleType.ORDERS);
		assertTrue(values[1] == ModuleType.LOGS);
		assertTrue(values[2] == ModuleType.INVENTORY);
		assertTrue(values[3] == ModuleType.BILLING);
		assertTrue(values[4] == ModuleType.REPORTS);
		assertTrue(values[5] == ModuleType.MATERIAL_INSTRUCTIONS);
		assertTrue(values[6] == ModuleType.CLOCK_TEMPLATES);
		assertTrue(values[7] == ModuleType.USER_MANAGEMENT);
		assertTrue(values[8] == ModuleType.SYSTEM_SETTINGS);
		assertTrue(values[9] == ModuleType.LIBRETIME_INTEGRATION);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(ModuleType.ORDERS, ModuleType.valueOf("ORDERS"));
		assertEquals(ModuleType.LOGS, ModuleType.valueOf("LOGS"));
		assertEquals(ModuleType.INVENTORY, ModuleType.valueOf("INVENTORY"));
		assertEquals(ModuleType.BILLING, ModuleType.valueOf("BILLING"));
		assertEquals(ModuleType.REPORTS, ModuleType.valueOf("REPORTS"));
		assertEquals(ModuleType.MATERIAL_INSTRUCTIONS, ModuleType.valueOf("MATERIAL_INSTRUCTIONS"));
		assertEquals(ModuleType.CLOCK_TEMPLATES, ModuleType.valueOf("CLOCK_TEMPLATES"));
		assertEquals(ModuleType.USER_MANAGEMENT, ModuleType.valueOf("USER_MANAGEMENT"));
		assertEquals(ModuleType.SYSTEM_SETTINGS, ModuleType.valueOf("SYSTEM_SETTINGS"));
		assertEquals(ModuleType.LIBRETIME_INTEGRATION, ModuleType.valueOf("LIBRETIME_INTEGRATION"));
	}
}

