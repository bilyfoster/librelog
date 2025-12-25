package com.onelpro.librelog.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void now_When_Called_Expect_CurrentInstant() {
        Instant before = Instant.now();
        Instant result = DateUtils.now();
        Instant after = Instant.now();

        assertNotNull(result);
        assertTrue(result.isAfter(before.minusSeconds(1)) || result.equals(before));
        assertTrue(result.isBefore(after.plusSeconds(1)) || result.equals(after));
    }

    @Test
    void nowLocalDateTime_When_Called_Expect_CurrentLocalDateTime() {
        LocalDateTime result = DateUtils.nowLocalDateTime();

        assertNotNull(result);
        assertTrue(result.isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void toLocalDateTime_When_ValidInstant_Expect_ConvertedLocalDateTime() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        LocalDateTime result = DateUtils.toLocalDateTime(instant);

        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(10, result.getHour());
        assertEquals(30, result.getMinute());
    }

    @Test
    void toLocalDateTime_When_NullInstant_Expect_Null() {
        LocalDateTime result = DateUtils.toLocalDateTime(null);
        assertNull(result);
    }

    @Test
    void toInstant_When_ValidLocalDateTime_Expect_ConvertedInstant() {
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Instant result = DateUtils.toInstant(localDateTime);

        assertNotNull(result);
        assertTrue(result.isAfter(Instant.parse("2024-01-15T10:29:59Z")));
        assertTrue(result.isBefore(Instant.parse("2024-01-15T10:30:01Z")));
    }

    @Test
    void toInstant_When_NullLocalDateTime_Expect_Null() {
        Instant result = DateUtils.toInstant(null);
        assertNull(result);
    }

    @Test
    void formatDate_When_ValidLocalDate_Expect_FormattedString() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        String result = DateUtils.formatDate(date);

        assertEquals("2024-01-15", result);
    }

    @Test
    void formatDate_When_NullDate_Expect_Null() {
        String result = DateUtils.formatDate(null);
        assertNull(result);
    }

    @Test
    void formatDateTime_When_ValidLocalDateTime_Expect_FormattedString() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        String result = DateUtils.formatDateTime(dateTime);

        assertEquals("2024-01-15 10:30:45", result);
    }

    @Test
    void formatDateTime_When_NullDateTime_Expect_Null() {
        String result = DateUtils.formatDateTime(null);
        assertNull(result);
    }

    @Test
    void parseDate_When_ValidString_Expect_ParsedLocalDate() {
        String dateString = "2024-01-15";
        LocalDate result = DateUtils.parseDate(dateString);

        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
    }

    @Test
    void parseDate_When_InvalidString_Expect_Exception() {
        String invalidString = "invalid-date";
        assertThrows(DateTimeParseException.class, () -> DateUtils.parseDate(invalidString));
    }

    @Test
    void parseDate_When_NullString_Expect_Null() {
        LocalDate result = DateUtils.parseDate(null);
        assertNull(result);
    }

    @Test
    void parseDate_When_EmptyString_Expect_Null() {
        LocalDate result = DateUtils.parseDate("");
        assertNull(result);
    }

    @Test
    void parseDateTime_When_ValidString_Expect_ParsedLocalDateTime() {
        String dateTimeString = "2024-01-15 10:30:45";
        LocalDateTime result = DateUtils.parseDateTime(dateTimeString);

        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(10, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(45, result.getSecond());
    }

    @Test
    void parseDateTime_When_InvalidString_Expect_Exception() {
        String invalidString = "invalid-datetime";
        assertThrows(DateTimeParseException.class, () -> DateUtils.parseDateTime(invalidString));
    }

    @Test
    void parseDateTime_When_NullString_Expect_Null() {
        LocalDateTime result = DateUtils.parseDateTime(null);
        assertNull(result);
    }

    @Test
    void parseDateTime_When_EmptyString_Expect_Null() {
        LocalDateTime result = DateUtils.parseDateTime("");
        assertNull(result);
    }

    @Test
    void isBefore_When_Date1BeforeDate2_Expect_True() {
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 1, 16);
        assertTrue(DateUtils.isBefore(date1, date2));
    }

    @Test
    void isBefore_When_Date1AfterDate2_Expect_False() {
        LocalDate date1 = LocalDate.of(2024, 1, 16);
        LocalDate date2 = LocalDate.of(2024, 1, 15);
        assertFalse(DateUtils.isBefore(date1, date2));
    }

    @Test
    void isBefore_When_NullDate_Expect_False() {
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        assertFalse(DateUtils.isBefore(null, date1));
        assertFalse(DateUtils.isBefore(date1, null));
        assertFalse(DateUtils.isBefore(null, null));
    }

    @Test
    void isAfter_When_Date1AfterDate2_Expect_True() {
        LocalDate date1 = LocalDate.of(2024, 1, 16);
        LocalDate date2 = LocalDate.of(2024, 1, 15);
        assertTrue(DateUtils.isAfter(date1, date2));
    }

    @Test
    void isAfter_When_Date1BeforeDate2_Expect_False() {
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        LocalDate date2 = LocalDate.of(2024, 1, 16);
        assertFalse(DateUtils.isAfter(date1, date2));
    }

    @Test
    void isAfter_When_NullDate_Expect_False() {
        LocalDate date1 = LocalDate.of(2024, 1, 15);
        assertFalse(DateUtils.isAfter(null, date1));
        assertFalse(DateUtils.isAfter(date1, null));
        assertFalse(DateUtils.isAfter(null, null));
    }
}

