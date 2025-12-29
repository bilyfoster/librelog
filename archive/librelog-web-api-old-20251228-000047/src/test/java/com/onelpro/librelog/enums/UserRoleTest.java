package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRoleTest {

    @Test
    void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
        UserRole[] values = UserRole.values();
        assertEquals(4, values.length, "UserRole should have exactly 4 values");
        assertTrue(values[0] == UserRole.ADMIN);
        assertTrue(values[1] == UserRole.PRODUCER);
        assertTrue(values[2] == UserRole.DJ);
        assertTrue(values[3] == UserRole.SALES);
    }

    @Test
    void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
        assertEquals(UserRole.PRODUCER, UserRole.valueOf("PRODUCER"));
        assertEquals(UserRole.DJ, UserRole.valueOf("DJ"));
        assertEquals(UserRole.SALES, UserRole.valueOf("SALES"));
    }
}

