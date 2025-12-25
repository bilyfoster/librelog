package com.onelpro.librelog.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnauthorizedExceptionTest {

    @Test
    void constructor_When_MessageProvided_Expect_ExceptionWithMessage() {
        String message = "Unauthorized access";
        UnauthorizedException exception = new UnauthorizedException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_When_MessageAndCauseProvided_Expect_ExceptionWithMessageAndCause() {
        String message = "Unauthorized access";
        Throwable cause = new RuntimeException("Root cause");
        UnauthorizedException exception = new UnauthorizedException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}

