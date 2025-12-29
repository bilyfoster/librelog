package com.onelpro.librelog.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Custom info contributor to expose application version and details.
 */
@Component
public class AppInfoContributor implements InfoContributor {

	private final Environment environment;

	public AppInfoContributor(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("app", java.util.Map.of(
				"name", environment.getProperty("info.app.name", "LibreLog"),
				"description", environment.getProperty("info.app.description", "Broadcast Traffic Management System"),
				"version", environment.getProperty("info.app.version", "Unknown")
		));
	}

}

