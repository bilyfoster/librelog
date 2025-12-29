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
 * Root controller to handle requests to the base path.
 * Serves the welcome page from static resources.
 */
@RestController
public class RootController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> root() {
        try {
            Resource resource = new ClassPathResource("static/index.html");
            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(html);
        } catch (IOException e) {
            // Fallback to simple HTML if file not found
            String fallback = "<!DOCTYPE html><html><head><title>LibreLog</title></head>" +
                    "<body style='font-family: Arial; text-align: center; padding: 50px; background: #1a1a2e; color: #f5f5f5;'>" +
                    "<h1>LibreLog</h1><p>Radio Automation & Scheduling System</p>" +
                    "<p><a href='/login.html' style='color: #667eea;'>Login</a></p>" +
                    "</body></html>";
            return ResponseEntity.ok(fallback);
        }
    }
}

