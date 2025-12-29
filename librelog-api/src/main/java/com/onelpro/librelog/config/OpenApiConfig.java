package com.onelpro.librelog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * 
 * Configured with dark theme support for professional appearance.
 */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("LibreLog API")
						.version("0.0.1-SNAPSHOT")
						.description("LibreLog - WideOrbit-like ERP System for Broadcasters. " +
								"Complete lifecycle management from inventory through order management, " +
								"traffic scheduling, placement automation, and financial operations.")
						.contact(new Contact()
								.name("LibreLog Team")
								.email("support@librelog.com"))
						.license(new License()
								.name("Proprietary")
								.url("https://librelog.com/license")))
				.servers(List.of(
						new Server().url("http://localhost:8080").description("Local Development Server"),
						new Server().url("https://api.librelog.com").description("Production Server")
				));
	}

}

