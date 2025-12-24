package com.onelpro.librelog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic application context loading tests using H2 in-memory database.
 * 
 * H2 is used here for fast, lightweight tests that don't require
 * PostgreSQL-specific features like JSON column types.
 * 
 * For tests that require PostgreSQL features (e.g., JSON columns),
 * use {@link LibreLogApplicationPostgresTests} instead.
 */
@SpringBootTest
@TestPropertySource(properties = {
	"spring.testcontainers.enabled=false",
	"spring.datasource.url=jdbc:h2:mem:testdb",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"spring.liquibase.enabled=false"
})
class LibreLogApplicationTests {

	@Test
	void contextLoads_When_ApplicationStarts_Expect_ContextInitialized() {
		// Basic context loading test - doesn't require PostgreSQL features
		// This test verifies that the Spring application context loads successfully
		// with all required beans and configurations
		// The @SpringBootTest annotation ensures the full context is loaded
	}

}

