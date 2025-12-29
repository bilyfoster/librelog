package com.onelpro.librelog.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

	@Test
	void isValid_When_ValidPassword_Expect_True() {
		assertTrue(PasswordValidator.isValid("ValidPass123!"));
		assertTrue(PasswordValidator.isValid("MyP@ssw0rd"));
		assertTrue(PasswordValidator.isValid("Test1234#"));
	}

	@Test
	void isValid_When_NullPassword_Expect_False() {
		assertFalse(PasswordValidator.isValid(null));
	}

	@Test
	void isValid_When_EmptyPassword_Expect_False() {
		assertFalse(PasswordValidator.isValid(""));
	}

	@Test
	void isValid_When_TooShortPassword_Expect_False() {
		assertFalse(PasswordValidator.isValid("Short1!"));
	}

	@Test
	void isValid_When_TooLongPassword_Expect_False() {
		String longPassword = "A".repeat(129) + "1!";
		assertFalse(PasswordValidator.isValid(longPassword));
	}

	@Test
	void isValid_When_MissingUppercase_Expect_False() {
		assertFalse(PasswordValidator.isValid("lowercase123!"));
	}

	@Test
	void isValid_When_MissingLowercase_Expect_False() {
		assertFalse(PasswordValidator.isValid("UPPERCASE123!"));
	}

	@Test
	void isValid_When_MissingDigit_Expect_False() {
		assertFalse(PasswordValidator.isValid("NoDigitHere!"));
	}

	@Test
	void isValid_When_MissingSpecialCharacter_Expect_False() {
		assertFalse(PasswordValidator.isValid("NoSpecial123"));
	}

	@Test
	void getRequirements_When_Called_Expect_NonEmptyString() {
		String requirements = PasswordValidator.getRequirements();
		assertTrue(requirements != null && !requirements.isEmpty());
		assertTrue(requirements.contains("8"));
		assertTrue(requirements.contains("128"));
	}

}

