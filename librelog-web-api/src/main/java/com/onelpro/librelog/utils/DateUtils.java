package com.onelpro.librelog.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for date and time operations.
 */
public final class DateUtils {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT);

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the current UTC timestamp as Instant.
     *
     * @return current UTC timestamp
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Gets the current LocalDateTime in UTC.
     *
     * @return current LocalDateTime in UTC
     */
    public static LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }

    /**
     * Converts Instant to LocalDateTime in UTC.
     *
     * @param instant the instant to convert
     * @return LocalDateTime in UTC
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }

    /**
     * Converts LocalDateTime to Instant.
     *
     * @param localDateTime the LocalDateTime to convert
     * @return Instant
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    /**
     * Formats LocalDate to string using default format (yyyy-MM-dd).
     *
     * @param date the date to format
     * @return formatted date string
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * Formats LocalDateTime to string using default format (yyyy-MM-dd HH:mm:ss).
     *
     * @param dateTime the date-time to format
     * @return formatted date-time string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * Parses a date string to LocalDate using default format (yyyy-MM-dd).
     *
     * @param dateString the date string to parse
     * @return parsed LocalDate
     * @throws DateTimeParseException if the string cannot be parsed
     */
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateString, DATE_FORMATTER);
    }

    /**
     * Parses a date-time string to LocalDateTime using default format (yyyy-MM-dd HH:mm:ss).
     *
     * @param dateTimeString the date-time string to parse
     * @return parsed LocalDateTime
     * @throws DateTimeParseException if the string cannot be parsed
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeString, DATETIME_FORMATTER);
    }

    /**
     * Checks if a date is before another date.
     *
     * @param date1 the first date
     * @param date2 the second date
     * @return true if date1 is before date2
     */
    public static boolean isBefore(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isBefore(date2);
    }

    /**
     * Checks if a date is after another date.
     *
     * @param date1 the first date
     * @param date2 the second date
     * @return true if date1 is after date2
     */
    public static boolean isAfter(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isAfter(date2);
    }
}

