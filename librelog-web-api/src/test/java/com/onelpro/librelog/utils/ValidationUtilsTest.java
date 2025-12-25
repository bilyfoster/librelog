package com.onelpro.librelog.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void validateNotBlank_When_BlankString_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateNotBlank("", "field", errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("cannot be blank"));
    }

    @Test
    void validateNotBlank_When_NullString_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateNotBlank(null, "field", errors);
        assertEquals(1, errors.size());
    }

    @Test
    void validateNotBlank_When_WhitespaceString_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateNotBlank("   ", "field", errors);
        assertEquals(1, errors.size());
    }

    @Test
    void validateNotBlank_When_ValidString_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateNotBlank("test", "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateNotNull_When_NullObject_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateNotNull(null, "field", errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("cannot be null"));
    }

    @Test
    void validateNotNull_When_ValidObject_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateNotNull("test", "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateMinLength_When_StringTooShort_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMinLength("ab", 5, "field", errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("at least"));
    }

    @Test
    void validateMinLength_When_StringMeetsMinLength_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMinLength("abcde", 5, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateMinLength_When_NullString_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMinLength(null, 5, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateMaxLength_When_StringTooLong_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMaxLength("abcdef", 5, "field", errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("at most"));
    }

    @Test
    void validateMaxLength_When_StringWithinMaxLength_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMaxLength("abc", 5, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateMaxLength_When_NullString_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMaxLength(null, 5, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateLength_When_StringTooShort_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateLength("ab", 5, 10, "field", errors);
        assertEquals(1, errors.size());
    }

    @Test
    void validateLength_When_StringTooLong_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateLength("abcdefghijk", 5, 10, "field", errors);
        assertEquals(1, errors.size());
    }

    @Test
    void validateLength_When_StringWithinRange_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateLength("abcdef", 5, 10, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateRange_When_ValueBelowMin_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateRange(3, 5, 10, "field", errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("between"));
    }

    @Test
    void validateRange_When_ValueAboveMax_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateRange(15, 5, 10, "field", errors);
        assertEquals(1, errors.size());
    }

    @Test
    void validateRange_When_ValueWithinRange_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateRange(7, 5, 10, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateRange_When_NullValue_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateRange(null, 5, 10, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateMin_When_ValueBelowMin_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMin(3, 5, "field", errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("at least"));
    }

    @Test
    void validateMin_When_ValueMeetsMin_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMin(5, 5, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateMax_When_ValueAboveMax_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMax(15, 10, "field", errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("at most"));
    }

    @Test
    void validateMax_When_ValueWithinMax_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateMax(5, 10, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateUUID_When_ValidUUID_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateUUID("550e8400-e29b-41d4-a716-446655440000", "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateUUID_When_InvalidUUID_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateUUID("invalid-uuid", "field", errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("valid UUID"));
    }

    @Test
    void validateUUID_When_NullValue_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateUUID(null, "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateEmail_When_ValidEmail_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateEmail("test@example.com", "field", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateEmail_When_InvalidEmail_Expect_ErrorAdded() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateEmail("invalid-email", "field", errors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("valid email"));
    }

    @Test
    void validateEmail_When_NullValue_Expect_NoError() {
        List<String> errors = new ArrayList<>();
        ValidationUtils.validateEmail(null, "field", errors);
        assertTrue(errors.isEmpty());
    }
}

