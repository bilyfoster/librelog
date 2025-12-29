package com.onelpro.librelog.controllers;

import com.onelpro.librelog.config.VersionInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for version information.
 */
@Slf4j
@RestController
@RequestMapping("/api/version")
@Tag(name = "Version", description = "Application version information")
public class VersionController {

    private final VersionInfo versionInfo;

    public VersionController(VersionInfo versionInfo) {
        this.versionInfo = versionInfo;
    }

    @GetMapping
    @Operation(summary = "Get version information", description = "Returns application version and git commit information")
    public ResponseEntity<Map<String, String>> getVersion() {
        Map<String, String> version = new HashMap<>();
        version.put("version", versionInfo.getVersion());
        version.put("gitCommit", versionInfo.getGitCommit());
        version.put("gitCommitShort", versionInfo.getGitCommitShort());
        version.put("display", versionInfo.getVersionDisplay());
        return ResponseEntity.ok(version);
    }
}

