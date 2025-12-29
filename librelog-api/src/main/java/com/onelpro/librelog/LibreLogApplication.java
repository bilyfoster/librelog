package com.onelpro.librelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application entry point for LibreLog.
 * 
 * LibreLog is a WideOrbit-like ERP system for broadcasters, managing
 * the complete lifecycle from inventory definition through order management,
 * traffic scheduling, placement automation, and financial operations.
 */
@SpringBootApplication
public class LibreLogApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibreLogApplication.class, args);
	}

}

