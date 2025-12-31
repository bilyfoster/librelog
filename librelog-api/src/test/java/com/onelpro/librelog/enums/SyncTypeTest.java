package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncTypeTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		SyncType[] values = SyncType.values();
		assertEquals(5, values.length, "SyncType should have exactly 5 values");
		assertTrue(values[0] == SyncType.FILE_UPLOAD);
		assertTrue(values[1] == SyncType.FILE_DOWNLOAD);
		assertTrue(values[2] == SyncType.BATCH_SYNC);
		assertTrue(values[3] == SyncType.LOG_EXPORT);
		assertTrue(values[4] == SyncType.MANUAL);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(SyncType.FILE_UPLOAD, SyncType.valueOf("FILE_UPLOAD"));
		assertEquals(SyncType.FILE_DOWNLOAD, SyncType.valueOf("FILE_DOWNLOAD"));
		assertEquals(SyncType.BATCH_SYNC, SyncType.valueOf("BATCH_SYNC"));
		assertEquals(SyncType.LOG_EXPORT, SyncType.valueOf("LOG_EXPORT"));
		assertEquals(SyncType.MANUAL, SyncType.valueOf("MANUAL"));
	}

}

