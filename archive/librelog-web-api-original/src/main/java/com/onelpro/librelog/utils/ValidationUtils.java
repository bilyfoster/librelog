package com.onelpro.librelog.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for validation operations.
 */
public final class ValidationUtils {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
    );

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates that a string is not null or blank.
     *
     * @param value the value to validate
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateNotBlank(String value, String fieldName, List<String> errors) {
        if (StringUtils.isBlank(value)) {
            errors.add(fieldName + " cannot be blank");
        }
    }

    /**
     * Validates that an object is not null.
     *
     * @param value the value to validate
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateNotNull(Object value, String fieldName, List<String> errors) {
        if (value == null) {
            errors.add(fieldName + " cannot be null");
        }
    }

    /**
     * Validates that a string matches a minimum length.
     *
     * @param value the value to validate
     * @param minLength the minimum length
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateMinLength(String value, int minLength, String fieldName, List<String> errors) {
        if (value != null && value.length() < minLength) {
            errors.add(fieldName + " must be at least " + minLength + " characters long");
        }
    }

    /**
     * Validates that a string matches a maximum length.
     *
     * @param value the value to validate
     * @param maxLength the maximum length
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateMaxLength(String value, int maxLength, String fieldName, List<String> errors) {
        if (value != null && value.length() > maxLength) {
            errors.add(fieldName + " must be at most " + maxLength + " characters long");
        }
    }

    /**
     * Validates that a string matches a length range.
     *
     * @param value the value to validate
     * @param minLength the minimum length
     * @param maxLength the maximum length
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateLength(String value, int minLength, int maxLength, String fieldName, List<String> errors) {
        if (value != null) {
            if (value.length() < minLength) {
                errors.add(fieldName + " must be at least " + minLength + " characters long");
            }
            if (value.length() > maxLength) {
                errors.add(fieldName + " must be at most " + maxLength + " characters long");
            }
        }
    }

    /**
     * Validates that a number is within a range.
     *
     * @param value the value to validate
     * @param min the minimum value
     * @param max the maximum value
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateRange(Number value, Number min, Number max, String fieldName, List<String> errors) {
        if (value != null) {
            double doubleValue = value.doubleValue();
            double doubleMin = min.doubleValue();
            double doubleMax = max.doubleValue();
            if (doubleValue < doubleMin || doubleValue > doubleMax) {
                errors.add(fieldName + " must be between " + min + " and " + max);
            }
        }
    }

    /**
     * Validates that a number is greater than or equal to a minimum.
     *
     * @param value the value to validate
     * @param min the minimum value
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateMin(Number value, Number min, String fieldName, List<String> errors) {
        if (value != null && value.doubleValue() < min.doubleValue()) {
            errors.add(fieldName + " must be at least " + min);
        }
    }

    /**
     * Validates that a number is less than or equal to a maximum.
     *
     * @param value the value to validate
     * @param max the maximum value
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateMax(Number value, Number max, String fieldName, List<String> errors) {
        if (value != null && value.doubleValue() > max.doubleValue()) {
            errors.add(fieldName + " must be at most " + max);
        }
    }

    /**
     * Validates that a string is a valid UUID format.
     *
     * @param value the value to validate
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateUUID(String value, String fieldName, List<String> errors) {
        if (value != null && !UUID_PATTERN.matcher(value).matches()) {
            errors.add(fieldName + " must be a valid UUID");
        }
    }

    /**
     * Validates that a string is a valid email format.
     *
     * @param value the value to validate
     * @param fieldName the name of the field for error messages
     * @param errors list to add errors to
     */
    public static void validateEmail(String value, String fieldName, List<String> errors) {
        if (value != null && !StringUtils.isValidEmail(value)) {
            errors.add(fieldName + " must be a valid email address");
        }
    }

    /**
     * Validates multiple conditions and returns a list of errors.
     *
     * @param validations list of validation functions to run
     * @return list of validation errors
     */
    public static List<String> validate(Runnable... validations) {
        List<String> errors = new ArrayList<>();
        for (Runnable validation : validations) {
            validation.run();
        }
        return errors;
    }
}

