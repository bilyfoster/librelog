package com.onelpro.librelog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database and JPA configuration.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.onelpro.librelog.repositories")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    // JPA configuration is handled via application.properties
    // This class enables JPA repositories, auditing, and transaction management
}

