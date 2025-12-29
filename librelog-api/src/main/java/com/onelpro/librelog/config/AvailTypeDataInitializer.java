package com.onelpro.librelog.config;

import com.onelpro.librelog.models.AvailType;
import com.onelpro.librelog.repositories.AvailTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Data initializer for default avail types.
 * Seeds the database with common avail types if they don't already exist.
 */
@Component
@Order(1)
public class AvailTypeDataInitializer implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(AvailTypeDataInitializer.class);

	private final AvailTypeRepository availTypeRepository;

	public AvailTypeDataInitializer(AvailTypeRepository availTypeRepository) {
		this.availTypeRepository = availTypeRepository;
	}

	@Override
	public void run(String... args) {
		logger.info("Initializing default avail types...");

		List<AvailTypeData> defaultAvailTypes = Arrays.asList(
				new AvailTypeData("General", "General commercial break - all ads allowed", true),
				new AvailTypeData("Premium", "Premium commercial break - high-value ads only", true),
				new AvailTypeData("Weather Sponsor Only", "Weather sponsor exclusivity - weather-related ads only", true),
				new AvailTypeData("Sports Content Only", "Sports content exclusivity - sports-related ads only", true)
		);

		for (AvailTypeData data : defaultAvailTypes) {
			availTypeRepository.findByNameIgnoreCase(data.name)
					.orElseGet(() -> {
						logger.info("Creating default avail type: {}", data.name);
						AvailType availType = AvailType.builder()
								.name(data.name)
								.description(data.description)
								.isActive(data.isActive)
								.createdAt(LocalDateTime.now())
								.updatedAt(LocalDateTime.now())
								.build();
						return availTypeRepository.save(availType);
					});
		}

		logger.info("Default avail types initialization complete");
	}

	private static class AvailTypeData {
		final String name;
		final String description;
		final Boolean isActive;

		AvailTypeData(String name, String description, Boolean isActive) {
			this.name = name;
			this.description = description;
			this.isActive = isActive;
		}
	}

}

