package com.onelpro.librelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application entry point for LibreLog.
 * 
 * LibreLog is a professional radio traffic, scheduling, and automation system
 * built to integrate with LibreTime and AzuraCast for GayPHX Radio.
 */
@SpringBootApplication
@EnableScheduling
public class LibreLogApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibreLogApplication.class, args);
	}

}

