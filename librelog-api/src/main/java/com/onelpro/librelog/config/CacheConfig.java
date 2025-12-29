package com.onelpro.librelog.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for permission and session caching.
 * Uses in-memory cache with 5-minute TTL (handled by cache eviction).
 */
@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
		cacheManager.setCacheNames(java.util.Arrays.asList(
				"permissions",
				"userStations",
				"stationAccess",
				"effectivePermissions",
				"sessions"
		));
		return cacheManager;
	}

}

