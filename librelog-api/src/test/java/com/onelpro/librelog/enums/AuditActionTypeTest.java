package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuditActionTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		AuditActionType[] values = AuditActionType.values();
		assertEquals(11, values.length, "AuditActionType should have exactly 11 values");
		assertTrue(values[0] == AuditActionType.CREATE);
		assertTrue(values[1] == AuditActionType.UPDATE);
		assertTrue(values[2] == AuditActionType.DELETE);
		assertTrue(values[3] == AuditActionType.LOGIN);
		assertTrue(values[4] == AuditActionType.LOGOUT);
		assertTrue(values[5] == AuditActionType.PERMISSION_CHANGE);
		assertTrue(values[6] == AuditActionType.STATION_ASSIGNMENT);
		assertTrue(values[7] == AuditActionType.ROLE_ASSIGNMENT);
		assertTrue(values[8] == AuditActionType.IMPERSONATION_START);
		assertTrue(values[9] == AuditActionType.IMPERSONATION_END);
		assertTrue(values[10] == AuditActionType.SESSION_TERMINATED);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(AuditActionType.CREATE, AuditActionType.valueOf("CREATE"));
		assertEquals(AuditActionType.UPDATE, AuditActionType.valueOf("UPDATE"));
		assertEquals(AuditActionType.DELETE, AuditActionType.valueOf("DELETE"));
		assertEquals(AuditActionType.LOGIN, AuditActionType.valueOf("LOGIN"));
		assertEquals(AuditActionType.LOGOUT, AuditActionType.valueOf("LOGOUT"));
		assertEquals(AuditActionType.PERMISSION_CHANGE, AuditActionType.valueOf("PERMISSION_CHANGE"));
		assertEquals(AuditActionType.STATION_ASSIGNMENT, AuditActionType.valueOf("STATION_ASSIGNMENT"));
		assertEquals(AuditActionType.ROLE_ASSIGNMENT, AuditActionType.valueOf("ROLE_ASSIGNMENT"));
		assertEquals(AuditActionType.IMPERSONATION_START, AuditActionType.valueOf("IMPERSONATION_START"));
		assertEquals(AuditActionType.IMPERSONATION_END, AuditActionType.valueOf("IMPERSONATION_END"));
		assertEquals(AuditActionType.SESSION_TERMINATED, AuditActionType.valueOf("SESSION_TERMINATED"));
	}
}

