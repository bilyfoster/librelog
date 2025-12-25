package com.onelpro.librelog.exceptions;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationExceptionTest {

    @Test
    void constructor_When_MessageProvided_Expect_ExceptionWithMessageAndSingleError() {
        String message = "Validation failed";
        ValidationException exception = new ValidationException(message);

        assertEquals(message, exception.getMessage());
        assertEquals(1, exception.getErrors().size());
        assertEquals(message, exception.getErrors().get(0));
    }

    @Test
    void constructor_When_MessageAndErrorsProvided_Expect_ExceptionWithErrors() {
        String message = "Validation failed";
        List<String> errors = Arrays.asList("Error 1", "Error 2", "Error 3");
        ValidationException exception = new ValidationException(message, errors);

        assertEquals(message, exception.getMessage());
        assertEquals(3, exception.getErrors().size());
        assertEquals(errors, exception.getErrors());
    }

    @Test
    void constructor_When_NullErrorsProvided_Expect_SingleErrorFromMessage() {
        String message = "Validation failed";
        @SuppressWarnings("unchecked")
        List<String> nullList = null;
        ValidationException exception = new ValidationException(message, nullList);

        assertEquals(message, exception.getMessage());
        assertEquals(1, exception.getErrors().size());
        assertEquals(message, exception.getErrors().get(0));
    }

    @Test
    void constructor_When_MessageAndCauseProvided_Expect_ExceptionWithMessageAndCause() {
        String message = "Validation failed";
        Throwable cause = new RuntimeException("Root cause");
        ValidationException exception = new ValidationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(1, exception.getErrors().size());
    }

    @Test
    void getErrors_When_Called_Expect_ImmutableList() {
        String message = "Validation failed";
        List<String> errors = Arrays.asList("Error 1", "Error 2");
        ValidationException exception = new ValidationException(message, errors);

        List<String> returnedErrors = exception.getErrors();
        assertThrows(UnsupportedOperationException.class, () -> returnedErrors.add("Error 3"));
    }
}

