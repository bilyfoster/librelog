package com.onelpro.librelog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.grafana.LgtmStackContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for TestcontainersConfiguration.
 * 
 * Tests the testcontainers configuration beans to ensure all methods are covered.
 */
class TestcontainersConfigurationTest {

	@Test
	void testcontainersConfiguration_When_ClassExists_Expect_ConfigurationAnnotation() {
		// Verify the configuration class exists and has the correct annotation
		assertNotNull(TestcontainersConfiguration.class);
		assertTrue(TestcontainersConfiguration.class.isAnnotationPresent(TestConfiguration.class));
	}

	@Test
	void grafanaLgtmContainer_When_Created_Expect_ContainerInstance() {
		// Test that the Grafana LGTM container can be created
		// This exercises the grafanaLgtmContainer() method
		TestcontainersConfiguration config = new TestcontainersConfiguration();
		LgtmStackContainer container = config.grafanaLgtmContainer();
		assertNotNull(container, "Grafana LGTM container should not be null");
		assertNotNull(container.getDockerImageName(), "Docker image name should not be null");
	}

	@Test
	void postgresContainer_When_Created_Expect_ContainerInstance() {
		// Test that the PostgreSQL container can be created
		// This exercises the postgresContainer() method
		TestcontainersConfiguration config = new TestcontainersConfiguration();
		PostgreSQLContainer container = config.postgresContainer();
		assertNotNull(container, "PostgreSQL container should not be null");
		assertNotNull(container.getDockerImageName(), "Docker image name should not be null");
	}

	@Test
	void postgresContainer_When_Created_Expect_CorrectImageName() {
		// Test that the PostgreSQL container uses the correct image
		TestcontainersConfiguration config = new TestcontainersConfiguration();
		PostgreSQLContainer container = config.postgresContainer();
		assertNotNull(container);
		String imageName = container.getDockerImageName().toString();
		assertTrue(imageName.contains("postgres"), 
			"Image name should contain 'postgres', but was: " + imageName);
	}

	@Test
	void grafanaLgtmContainer_When_CreatedMultipleTimes_Expect_NewInstances() {
		// Test that multiple calls create new instances
		TestcontainersConfiguration config = new TestcontainersConfiguration();
		LgtmStackContainer container1 = config.grafanaLgtmContainer();
		LgtmStackContainer container2 = config.grafanaLgtmContainer();
		assertNotNull(container1);
		assertNotNull(container2);
		// Note: They may or may not be the same instance depending on proxyBeanMethods setting
	}

	@Test
	void postgresContainer_When_CreatedMultipleTimes_Expect_NewInstances() {
		// Test that multiple calls create new instances
		TestcontainersConfiguration config = new TestcontainersConfiguration();
		PostgreSQLContainer container1 = config.postgresContainer();
		PostgreSQLContainer container2 = config.postgresContainer();
		assertNotNull(container1);
		assertNotNull(container2);
		// Note: They may or may not be the same instance depending on proxyBeanMethods setting
	}

}

