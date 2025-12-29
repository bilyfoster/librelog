package com.onelpro.librelog.controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to serve the dashboard page.
 */
@RestController
public class DashboardController {

    @GetMapping(value = "/dashboard.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> dashboard() {
        try {
            Resource resource = new ClassPathResource("static/dashboard.html");
            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(html);
        } catch (IOException e) {
            // Fallback to simple HTML if file not found
            String fallback = "<!DOCTYPE html><html><head><title>Dashboard - LibreLog</title></head>" +
                    "<body style='font-family: Arial; text-align: center; padding: 50px; background: #1a1a2e; color: #f5f5f5;'>" +
                    "<h1>Dashboard</h1><p>Dashboard page not found</p>" +
                    "</body></html>";
            return ResponseEntity.ok(fallback);
        }
    }
}

