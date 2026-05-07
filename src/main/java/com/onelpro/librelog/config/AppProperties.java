package com.onelpro.librelog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "librelog")
@Data
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Admin admin = new Admin();
    private final Encryption encryption = new Encryption();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMinutes = 480;
    }

    @Data
    public static class Admin {
        private String seedEmail;
        private String seedPassword;
        private String seedName;
    }

    @Data
    public static class Encryption {
        private String key;
    }
}
