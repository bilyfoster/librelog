package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomationCommandTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		AutomationCommandType[] values = AutomationCommandType.values();
		assertEquals(8, values.length, "AutomationCommandType should have exactly 8 values");
		assertTrue(values[0] == AutomationCommandType.SWITCH_TO_SATELLITE);
		assertTrue(values[1] == AutomationCommandType.START_RECORDING);
		assertTrue(values[2] == AutomationCommandType.ENABLE_LIVE_MIX);
		assertTrue(values[3] == AutomationCommandType.DISABLE_LIVE_MIX);
		assertTrue(values[4] == AutomationCommandType.SWITCH_TO_NETWORK);
		assertTrue(values[5] == AutomationCommandType.TRIGGER_EAS);
		assertTrue(values[6] == AutomationCommandType.START_STREAMING);
		assertTrue(values[7] == AutomationCommandType.STOP_STREAMING);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(AutomationCommandType.SWITCH_TO_SATELLITE, AutomationCommandType.valueOf("SWITCH_TO_SATELLITE"));
		assertEquals(AutomationCommandType.START_RECORDING, AutomationCommandType.valueOf("START_RECORDING"));
		assertEquals(AutomationCommandType.ENABLE_LIVE_MIX, AutomationCommandType.valueOf("ENABLE_LIVE_MIX"));
		assertEquals(AutomationCommandType.DISABLE_LIVE_MIX, AutomationCommandType.valueOf("DISABLE_LIVE_MIX"));
		assertEquals(AutomationCommandType.SWITCH_TO_NETWORK, AutomationCommandType.valueOf("SWITCH_TO_NETWORK"));
		assertEquals(AutomationCommandType.TRIGGER_EAS, AutomationCommandType.valueOf("TRIGGER_EAS"));
		assertEquals(AutomationCommandType.START_STREAMING, AutomationCommandType.valueOf("START_STREAMING"));
		assertEquals(AutomationCommandType.STOP_STREAMING, AutomationCommandType.valueOf("STOP_STREAMING"));
	}

}

