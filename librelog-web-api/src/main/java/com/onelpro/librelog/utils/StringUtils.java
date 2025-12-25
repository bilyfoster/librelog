package com.onelpro.librelog.utils;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Utility class for string operations.
 */
public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Checks if a string is null or empty (after trimming).
     *
     * @param str the string to check
     * @return true if the string is null or empty
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks if a string is not null and not empty (after trimming).
     *
     * @param str the string to check
     * @return true if the string is not blank
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Trims a string and returns null if the result is empty.
     *
     * @param str the string to trim
     * @return trimmed string or null if empty
     */
    public static String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Trims a string and returns empty string if null.
     *
     * @param str the string to trim
     * @return trimmed string or empty string if null
     */
    public static String trimToEmpty(String str) {
        if (str == null) {
            return "";
        }
        return str.trim();
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str the string to capitalize
     * @return capitalized string or null if input is null
     */
    public static String capitalize(String str) {
        if (isBlank(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Converts a string to lowercase, handling null.
     *
     * @param str the string to convert
     * @return lowercase string or null if input is null
     */
    public static String toLowerCase(String str) {
        return str == null ? null : str.toLowerCase();
    }

    /**
     * Converts a string to uppercase, handling null.
     *
     * @param str the string to convert
     * @return uppercase string or null if input is null
     */
    public static String toUpperCase(String str) {
        return str == null ? null : str.toUpperCase();
    }

    /**
     * Joins a collection of strings with a delimiter.
     *
     * @param collection the collection to join
     * @param delimiter the delimiter to use
     * @return joined string
     */
    public static String join(Collection<String> collection, String delimiter) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        return collection.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(delimiter));
    }

    /**
     * Checks if a string contains only digits.
     *
     * @param str the string to check
     * @return true if the string contains only digits
     */
    public static boolean isNumeric(String str) {
        if (isBlank(str)) {
            return false;
        }
        return str.matches("\\d+");
    }

    /**
     * Checks if a string is a valid email format (basic validation).
     *
     * @param email the email string to validate
     * @return true if the email format is valid
     */
    public static boolean isValidEmail(String email) {
        if (isBlank(email)) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}

