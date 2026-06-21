package com.onelpro.librelog.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class VersionController {

    private final BuildProperties buildProperties;

    public VersionController(ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.buildProperties = buildPropertiesProvider.getIfAvailable();
    }

    /**
     * Public build identity (from {@code spring-boot:build-info}). Shown in the dashboard footer.
     */
    @GetMapping(value = "/api/version", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> version() {
        Map<String, String> body = new LinkedHashMap<>();
        if (buildProperties != null) {
            body.put("version", buildProperties.getVersion());
            body.put("artifact", buildProperties.getArtifact());
            body.put("group", buildProperties.getGroup());
            body.put("name", buildProperties.getName());
            body.put("time", buildProperties.getTime() != null ? buildProperties.getTime().toString() : "");
        } else {
            body.put("version", "unknown");
            body.put("artifact", "");
            body.put("group", "");
            body.put("name", "");
            body.put("time", "");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).mustRevalidate())
                .body(body);
    }
}
