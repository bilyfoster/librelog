package com.onelpro.librelog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests that require PostgreSQL features like JSON column types.
 * 
 * These tests use Testcontainers to spin up a PostgreSQL container.
 * Requires Docker to be running.
 * 
 * For basic context loading tests that don't need database features,
 * use {@link LibreLogApplicationTests} instead (uses H2 in-memory database).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
	"spring.liquibase.enabled=true"
})
class LibreLogApplicationPostgresTests {

	@Test
	void contextLoads_When_ApplicationStartsWithPostgres_Expect_ContextInitialized() {
		// This test verifies the application context loads with PostgreSQL
		// Use this test class as a base for tests that require JSON columns
		// or other PostgreSQL-specific features
		// The @SpringBootTest annotation ensures the full context is loaded
		// with PostgreSQL via Testcontainers
	}

}

