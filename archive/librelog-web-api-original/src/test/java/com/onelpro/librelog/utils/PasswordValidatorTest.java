package com.onelpro.librelog.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    @Test
    void validate_When_ValidPassword_Expect_NoErrors() {
        String password = "ValidPass123!";
        List<String> errors = PasswordValidator.validate(password);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validate_When_NullPassword_Expect_Error() {
        List<String> errors = PasswordValidator.validate(null);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("cannot be empty"));
    }

    @Test
    void validate_When_EmptyPassword_Expect_Error() {
        List<String> errors = PasswordValidator.validate("");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("cannot be empty"));
    }

    @Test
    void validate_When_TooShortPassword_Expect_Error() {
        String password = "Short1!";
        List<String> errors = PasswordValidator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("at least")));
    }

    @Test
    void validate_When_TooLongPassword_Expect_Error() {
        String password = "A".repeat(129) + "1!";
        List<String> errors = PasswordValidator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("at most")));
    }

    @Test
    void validate_When_NoUppercase_Expect_Error() {
        String password = "lowercase123!";
        List<String> errors = PasswordValidator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("uppercase")));
    }

    @Test
    void validate_When_NoLowercase_Expect_Error() {
        String password = "UPPERCASE123!";
        List<String> errors = PasswordValidator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lowercase")));
    }

    @Test
    void validate_When_NoDigit_Expect_Error() {
        String password = "NoDigitHere!";
        List<String> errors = PasswordValidator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("digit")));
    }

    @Test
    void validate_When_NoSpecialCharacter_Expect_Error() {
        String password = "NoSpecialChar123";
        List<String> errors = PasswordValidator.validate(password);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("special character")));
    }

    @Test
    void validate_When_MultipleIssues_Expect_MultipleErrors() {
        String password = "short";
        List<String> errors = PasswordValidator.validate(password);
        assertTrue(errors.size() > 1);
    }

    @Test
    void isValid_When_ValidPassword_Expect_True() {
        String password = "ValidPass123!";
        assertTrue(PasswordValidator.isValid(password));
    }

    @Test
    void isValid_When_InvalidPassword_Expect_False() {
        String password = "short";
        assertFalse(PasswordValidator.isValid(password));
    }

    @Test
    void isValid_When_NullPassword_Expect_False() {
        assertFalse(PasswordValidator.isValid(null));
    }
}

