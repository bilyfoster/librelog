package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrackTypeTest {

    @Test
    void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
        TrackType[] values = TrackType.values();
        assertEquals(12, values.length, "TrackType should have exactly 12 values");
        assertTrue(values[0] == TrackType.MUS);
        assertTrue(values[1] == TrackType.ADV);
        assertTrue(values[2] == TrackType.PSA);
        assertTrue(values[3] == TrackType.LIN);
        assertTrue(values[4] == TrackType.INT);
        assertTrue(values[5] == TrackType.PRO);
        assertTrue(values[6] == TrackType.BED);
        assertTrue(values[7] == TrackType.SHO);
        assertTrue(values[8] == TrackType.IDS);
        assertTrue(values[9] == TrackType.COM);
        assertTrue(values[10] == TrackType.NEW);
        assertTrue(values[11] == TrackType.VOT);
    }

    @Test
    void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
        assertEquals(TrackType.MUS, TrackType.valueOf("MUS"));
        assertEquals(TrackType.ADV, TrackType.valueOf("ADV"));
        assertEquals(TrackType.PSA, TrackType.valueOf("PSA"));
        assertEquals(TrackType.LIN, TrackType.valueOf("LIN"));
        assertEquals(TrackType.INT, TrackType.valueOf("INT"));
        assertEquals(TrackType.PRO, TrackType.valueOf("PRO"));
        assertEquals(TrackType.BED, TrackType.valueOf("BED"));
        assertEquals(TrackType.SHO, TrackType.valueOf("SHO"));
        assertEquals(TrackType.IDS, TrackType.valueOf("IDS"));
        assertEquals(TrackType.COM, TrackType.valueOf("COM"));
        assertEquals(TrackType.NEW, TrackType.valueOf("NEW"));
        assertEquals(TrackType.VOT, TrackType.valueOf("VOT"));
    }
}

