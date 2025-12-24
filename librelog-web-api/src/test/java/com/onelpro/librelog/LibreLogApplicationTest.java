package com.onelpro.librelog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for LibreLogApplication main class.
 * 
 * Tests the application class structure and basic functionality.
 */
class LibreLogApplicationTest {

	@Test
	void applicationClass_When_Instantiated_Expect_ClassExists() {
		// Verify the application class exists and can be referenced
		assertNotNull(LibreLogApplication.class);
		assertTrue(LibreLogApplication.class.isAnnotationPresent(
			org.springframework.boot.autoconfigure.SpringBootApplication.class));
	}

	@Test
	void main_When_Called_Expect_ApplicationClassExists() {
		// Verify the main method exists and the class structure is correct
		Class<?>[] parameterTypes = {String[].class};
		try {
			LibreLogApplication.class.getMethod("main", parameterTypes);
			assertNotNull(LibreLogApplication.class);
		} catch (NoSuchMethodException e) {
			throw new AssertionError("main method not found", e);
		}
	}

}

