package com.onelpro.librelog.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BadRequestExceptionTest {

    @Test
    void constructor_When_MessageProvided_Expect_ExceptionWithMessage() {
        String message = "Bad request";
        BadRequestException exception = new BadRequestException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_When_MessageAndCauseProvided_Expect_ExceptionWithMessageAndCause() {
        String message = "Bad request";
        Throwable cause = new RuntimeException("Root cause");
        BadRequestException exception = new BadRequestException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}

