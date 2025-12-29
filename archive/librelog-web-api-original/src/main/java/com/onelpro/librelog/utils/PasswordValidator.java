package com.onelpro.librelog.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for password validation.
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*[0-9].*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*";

    private PasswordValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates a password against security requirements.
     *
     * @param password the password to validate
     * @return list of validation error messages, empty if password is valid
     */
    public static List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password cannot be empty");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("Password must be at most " + MAX_LENGTH + " characters long");
        }

        if (!password.matches(UPPERCASE_PATTERN)) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!password.matches(LOWERCASE_PATTERN)) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (!password.matches(DIGIT_PATTERN)) {
            errors.add("Password must contain at least one digit");
        }

        if (!password.matches(SPECIAL_CHAR_PATTERN)) {
            errors.add("Password must contain at least one special character");
        }

        return errors;
    }

    /**
     * Checks if a password meets all validation requirements.
     *
     * @param password the password to check
     * @return true if password is valid, false otherwise
     */
    public static boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}

