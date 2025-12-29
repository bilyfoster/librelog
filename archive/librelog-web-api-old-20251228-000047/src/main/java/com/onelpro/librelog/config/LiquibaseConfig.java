package com.onelpro.librelog.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Custom Liquibase configuration to handle changelog files from dependency JARs.
 * This bean overrides the default Spring Boot Liquibase auto-configuration
 * to properly handle changelog files located in dependency JARs.
 * 
 * Only created when spring.liquibase.enabled is true (default).
 */
@Configuration
@ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true", matchIfMissing = true)
public class LiquibaseConfig implements ResourceLoaderAware {

    @Autowired
    private DataSource dataSource;

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    @Primary
    public SpringLiquibase liquibase() throws IOException {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        
        // Get the resource and convert to URL string for Liquibase
        // This approach works for resources in both JARs and filesystem
        Resource changelogResource = resourceLoader.getResource("classpath:db/changelog/db.changelog-master-rebuild.xml");
        
        // Check if resource exists before trying to get URL
        if (!changelogResource.exists()) {
            throw new IllegalStateException(
                "Liquibase changelog file not found: classpath:db/changelog/db.changelog-master-rebuild.xml. " +
                "Make sure librelog-liquibase module is properly included as a dependency."
            );
        }
        
        String changelogUrl = changelogResource.getURL().toString();
        
        // Use the URL string - Liquibase will handle it
        liquibase.setChangeLog(changelogUrl);
        
        // Set the ResourceLoader so Liquibase can resolve relative paths in included changelog files
        // This is important for resolving <include> statements in the master changelog
        liquibase.setResourceLoader(resourceLoader);
        
        liquibase.setShouldRun(true);
        liquibase.setDropFirst(false);
        return liquibase;
    }
}
