package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncStatusTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		SyncStatus[] values = SyncStatus.values();
		assertEquals(5, values.length, "SyncStatus should have exactly 5 values");
		assertTrue(values[0] == SyncStatus.PENDING);
		assertTrue(values[1] == SyncStatus.SYNCING);
		assertTrue(values[2] == SyncStatus.SYNCED);
		assertTrue(values[3] == SyncStatus.FAILED);
		assertTrue(values[4] == SyncStatus.CONFLICT);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(SyncStatus.PENDING, SyncStatus.valueOf("PENDING"));
		assertEquals(SyncStatus.SYNCING, SyncStatus.valueOf("SYNCING"));
		assertEquals(SyncStatus.SYNCED, SyncStatus.valueOf("SYNCED"));
		assertEquals(SyncStatus.FAILED, SyncStatus.valueOf("FAILED"));
		assertEquals(SyncStatus.CONFLICT, SyncStatus.valueOf("CONFLICT"));
	}

}

