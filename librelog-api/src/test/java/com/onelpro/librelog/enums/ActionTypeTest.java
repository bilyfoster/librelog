package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		ActionType[] values = ActionType.values();
		assertEquals(4, values.length, "ActionType should have exactly 4 values");
		assertTrue(values[0] == ActionType.VIEW);
		assertTrue(values[1] == ActionType.CREATE);
		assertTrue(values[2] == ActionType.EDIT);
		assertTrue(values[3] == ActionType.DELETE);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(ActionType.VIEW, ActionType.valueOf("VIEW"));
		assertEquals(ActionType.CREATE, ActionType.valueOf("CREATE"));
		assertEquals(ActionType.EDIT, ActionType.valueOf("EDIT"));
		assertEquals(ActionType.DELETE, ActionType.valueOf("DELETE"));
	}
}

