package com.onelpro.librelog.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for password validation.
 */
public class PasswordValidator {

	private static final Logger logger = LoggerFactory.getLogger(PasswordValidator.class);

	private static final int MIN_LENGTH = 8;
	private static final int MAX_LENGTH = 128;
	private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
	private static final String LOWERCASE_PATTERN = ".*[a-z].*";
	private static final String DIGIT_PATTERN = ".*[0-9].*";
	private static final String SPECIAL_CHAR_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*";

	/**
	 * Validates a password according to security requirements.
	 * 
	 * @param password the password to validate
	 * @return true if password is valid, false otherwise
	 */
	public static boolean isValid(String password) {
		if (password == null || password.isEmpty()) {
			logger.debug("Password validation failed: password is null or empty");
			return false;
		}

		if (password.length() < MIN_LENGTH) {
			logger.debug("Password validation failed: password too short (minimum {} characters)", MIN_LENGTH);
			return false;
		}

		if (password.length() > MAX_LENGTH) {
			logger.debug("Password validation failed: password too long (maximum {} characters)", MAX_LENGTH);
			return false;
		}

		if (!password.matches(UPPERCASE_PATTERN)) {
			logger.debug("Password validation failed: missing uppercase letter");
			return false;
		}

		if (!password.matches(LOWERCASE_PATTERN)) {
			logger.debug("Password validation failed: missing lowercase letter");
			return false;
		}

		if (!password.matches(DIGIT_PATTERN)) {
			logger.debug("Password validation failed: missing digit");
			return false;
		}

		if (!password.matches(SPECIAL_CHAR_PATTERN)) {
			logger.debug("Password validation failed: missing special character");
			return false;
		}

		logger.debug("Password validation passed");
		return true;
	}

	/**
	 * Gets the password requirements as a human-readable string.
	 * 
	 * @return password requirements description
	 */
	public static String getRequirements() {
		return String.format(
				"Password must be between %d and %d characters long, " +
						"contain at least one uppercase letter, one lowercase letter, " +
						"one digit, and one special character.",
				MIN_LENGTH, MAX_LENGTH);
	}

}

