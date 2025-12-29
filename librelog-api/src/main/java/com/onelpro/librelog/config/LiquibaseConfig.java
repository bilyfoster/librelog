package com.onelpro.librelog.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

/**
 * Liquibase configuration for database migrations.
 */
@Configuration
public class LiquibaseConfig {

	@Bean
	public SpringLiquibase liquibase(
			DataSource dataSource,
			ResourceLoader resourceLoader,
			@Value("${spring.liquibase.change-log}") String changeLog) {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setDataSource(dataSource);
		liquibase.setChangeLog(changeLog);
		liquibase.setResourceLoader(resourceLoader);
		liquibase.setDropFirst(false);
		return liquibase;
	}

}

