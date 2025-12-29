package com.onelpro.librelog.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void isBlank_When_NullString_Expect_True() {
        assertTrue(StringUtils.isBlank(null));
    }

    @Test
    void isBlank_When_EmptyString_Expect_True() {
        assertTrue(StringUtils.isBlank(""));
    }

    @Test
    void isBlank_When_WhitespaceString_Expect_True() {
        assertTrue(StringUtils.isBlank("   "));
    }

    @Test
    void isBlank_When_NonBlankString_Expect_False() {
        assertFalse(StringUtils.isBlank("test"));
    }

    @Test
    void isNotBlank_When_NullString_Expect_False() {
        assertFalse(StringUtils.isNotBlank(null));
    }

    @Test
    void isNotBlank_When_EmptyString_Expect_False() {
        assertFalse(StringUtils.isNotBlank(""));
    }

    @Test
    void isNotBlank_When_WhitespaceString_Expect_False() {
        assertFalse(StringUtils.isNotBlank("   "));
    }

    @Test
    void isNotBlank_When_NonBlankString_Expect_True() {
        assertTrue(StringUtils.isNotBlank("test"));
    }

    @Test
    void trimToNull_When_NullString_Expect_Null() {
        assertNull(StringUtils.trimToNull(null));
    }

    @Test
    void trimToNull_When_EmptyString_Expect_Null() {
        assertNull(StringUtils.trimToNull(""));
    }

    @Test
    void trimToNull_When_WhitespaceString_Expect_Null() {
        assertNull(StringUtils.trimToNull("   "));
    }

    @Test
    void trimToNull_When_StringWithWhitespace_Expect_Trimmed() {
        assertEquals("test", StringUtils.trimToNull("  test  "));
    }

    @Test
    void trimToNull_When_NonWhitespaceString_Expect_SameString() {
        assertEquals("test", StringUtils.trimToNull("test"));
    }

    @Test
    void trimToEmpty_When_NullString_Expect_EmptyString() {
        assertEquals("", StringUtils.trimToEmpty(null));
    }

    @Test
    void trimToEmpty_When_EmptyString_Expect_EmptyString() {
        assertEquals("", StringUtils.trimToEmpty(""));
    }

    @Test
    void trimToEmpty_When_WhitespaceString_Expect_EmptyString() {
        assertEquals("", StringUtils.trimToEmpty("   "));
    }

    @Test
    void trimToEmpty_When_StringWithWhitespace_Expect_Trimmed() {
        assertEquals("test", StringUtils.trimToEmpty("  test  "));
    }

    @Test
    void trimToEmpty_When_NonWhitespaceString_Expect_SameString() {
        assertEquals("test", StringUtils.trimToEmpty("test"));
    }

    @Test
    void capitalize_When_NullString_Expect_Null() {
        assertNull(StringUtils.capitalize(null));
    }

    @Test
    void capitalize_When_EmptyString_Expect_EmptyString() {
        assertEquals("", StringUtils.capitalize(""));
    }

    @Test
    void capitalize_When_WhitespaceString_Expect_SameString() {
        assertEquals("   ", StringUtils.capitalize("   "));
    }

    @Test
    void capitalize_When_LowercaseString_Expect_Capitalized() {
        assertEquals("Test", StringUtils.capitalize("test"));
    }

    @Test
    void capitalize_When_AlreadyCapitalized_Expect_SameString() {
        assertEquals("Test", StringUtils.capitalize("Test"));
    }

    @Test
    void toLowerCase_When_NullString_Expect_Null() {
        assertNull(StringUtils.toLowerCase(null));
    }

    @Test
    void toLowerCase_When_UpperCaseString_Expect_LowerCase() {
        assertEquals("test", StringUtils.toLowerCase("TEST"));
    }

    @Test
    void toLowerCase_When_MixedCaseString_Expect_LowerCase() {
        assertEquals("test", StringUtils.toLowerCase("TeSt"));
    }

    @Test
    void toUpperCase_When_NullString_Expect_Null() {
        assertNull(StringUtils.toUpperCase(null));
    }

    @Test
    void toUpperCase_When_LowerCaseString_Expect_UpperCase() {
        assertEquals("TEST", StringUtils.toUpperCase("test"));
    }

    @Test
    void toUpperCase_When_MixedCaseString_Expect_UpperCase() {
        assertEquals("TEST", StringUtils.toUpperCase("TeSt"));
    }

    @Test
    void join_When_NullCollection_Expect_EmptyString() {
        assertEquals("", StringUtils.join(null, ","));
    }

    @Test
    void join_When_EmptyCollection_Expect_EmptyString() {
        assertEquals("", StringUtils.join(Collections.emptyList(), ","));
    }

    @Test
    void join_When_ValidCollection_Expect_JoinedString() {
        List<String> collection = Arrays.asList("a", "b", "c");
        assertEquals("a,b,c", StringUtils.join(collection, ","));
    }

    @Test
    void join_When_CollectionWithBlanks_Expect_BlanksFiltered() {
        List<String> collection = Arrays.asList("a", "", "b", "   ", "c");
        assertEquals("a,b,c", StringUtils.join(collection, ","));
    }

    @Test
    void join_When_CollectionWithNulls_Expect_NullsFiltered() {
        List<String> collection = Arrays.asList("a", null, "b", null, "c");
        assertEquals("a,b,c", StringUtils.join(collection, ","));
    }

    @Test
    void isNumeric_When_NullString_Expect_False() {
        assertFalse(StringUtils.isNumeric(null));
    }

    @Test
    void isNumeric_When_EmptyString_Expect_False() {
        assertFalse(StringUtils.isNumeric(""));
    }

    @Test
    void isNumeric_When_WhitespaceString_Expect_False() {
        assertFalse(StringUtils.isNumeric("   "));
    }

    @Test
    void isNumeric_When_NumericString_Expect_True() {
        assertTrue(StringUtils.isNumeric("123"));
    }

    @Test
    void isNumeric_When_NonNumericString_Expect_False() {
        assertFalse(StringUtils.isNumeric("abc"));
    }

    @Test
    void isNumeric_When_MixedString_Expect_False() {
        assertFalse(StringUtils.isNumeric("123abc"));
    }

    @Test
    void isValidEmail_When_NullString_Expect_False() {
        assertFalse(StringUtils.isValidEmail(null));
    }

    @Test
    void isValidEmail_When_EmptyString_Expect_False() {
        assertFalse(StringUtils.isValidEmail(""));
    }

    @Test
    void isValidEmail_When_ValidEmail_Expect_True() {
        assertTrue(StringUtils.isValidEmail("test@example.com"));
    }

    @Test
    void isValidEmail_When_InvalidEmail_Expect_False() {
        assertFalse(StringUtils.isValidEmail("invalid-email"));
    }

    @Test
    void isValidEmail_When_EmailWithoutDomain_Expect_False() {
        assertFalse(StringUtils.isValidEmail("test@"));
    }

    @Test
    void isValidEmail_When_EmailWithSubdomain_Expect_True() {
        assertTrue(StringUtils.isValidEmail("test@sub.example.com"));
    }
}

