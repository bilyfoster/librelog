package com.onelpro.librelog.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConflictExceptionTest {

    @Test
    void constructor_When_MessageProvided_Expect_ExceptionWithMessage() {
        String message = "Resource conflict";
        ConflictException exception = new ConflictException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_When_MessageAndCauseProvided_Expect_ExceptionWithMessageAndCause() {
        String message = "Resource conflict";
        Throwable cause = new RuntimeException("Root cause");
        ConflictException exception = new ConflictException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}

