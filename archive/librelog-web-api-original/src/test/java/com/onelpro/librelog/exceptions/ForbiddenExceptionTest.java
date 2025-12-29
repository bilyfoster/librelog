package com.onelpro.librelog.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ForbiddenExceptionTest {

    @Test
    void constructor_When_MessageProvided_Expect_ExceptionWithMessage() {
        String message = "Access forbidden";
        ForbiddenException exception = new ForbiddenException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_When_MessageAndCauseProvided_Expect_ExceptionWithMessageAndCause() {
        String message = "Access forbidden";
        Throwable cause = new RuntimeException("Root cause");
        ForbiddenException exception = new ForbiddenException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}

