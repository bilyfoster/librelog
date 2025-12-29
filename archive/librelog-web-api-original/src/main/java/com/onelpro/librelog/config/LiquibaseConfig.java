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
        
        // Get the changelog resource from the classpath
        Resource changelogResource = resourceLoader.getResource("classpath:db/changelog/db.changelog-master.xml");
        
        // Check if resource exists - for JAR resources, exists() may return false even if the resource is available
        // So we also try to check if we can get an InputStream or URL
        boolean resourceExists = false;
        try {
            resourceExists = changelogResource.exists();
            // If exists() returns false, try to get URL or InputStream to verify it's actually accessible
            if (!resourceExists) {
                try {
                    changelogResource.getURL();
                    resourceExists = true; // URL is accessible, resource exists
                } catch (IOException e) {
                    // Try InputStream as fallback
                    try (var ignored = changelogResource.getInputStream()) {
                        resourceExists = true;
                    } catch (IOException ex) {
                        resourceExists = false;
                    }
                }
            }
        } catch (Exception e) {
            // If we can't check, let Liquibase handle it - it will provide a better error message
            resourceExists = false;
        }
        
        if (!resourceExists) {
            throw new IllegalStateException(
                "Liquibase changelog file not found: classpath:db/changelog/db.changelog-master.xml. " +
                "Make sure librelog-liquibase module is properly built and included as a dependency. " +
                "Run 'mvn clean install -DskipTests' first to build all modules."
            );
        }
        
        // Set the ResourceLoader so Liquibase can resolve relative paths in included changelog files
        liquibase.setResourceLoader(resourceLoader);
        
        // When ResourceLoader is set, Liquibase expects a relative path (without classpath: prefix)
        // The ResourceLoader will handle resolving the resource from the classpath
        // This works for both JAR resources and filesystem resources
        liquibase.setChangeLog("db/changelog/db.changelog-master.xml");
        
        liquibase.setShouldRun(true);
        liquibase.setDropFirst(false);
        return liquibase;
    }
}


