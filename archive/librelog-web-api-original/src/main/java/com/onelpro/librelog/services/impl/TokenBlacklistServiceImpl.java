package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.services.JwtTokenService;
import com.onelpro.librelog.services.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of TokenBlacklistService.
 * Note: In a production environment with multiple instances, consider using Redis or a shared cache.
 */
@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistServiceImpl.class);

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final JwtTokenService jwtTokenService;

    public TokenBlacklistServiceImpl(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public void blacklistToken(String token) {
        if (token != null && !token.isEmpty()) {
            blacklistedTokens.add(token);
            logger.debug("Token blacklisted");
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return blacklistedTokens.contains(token);
    }

    @Override
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredTokens() {
        int initialSize = blacklistedTokens.size();
        blacklistedTokens.removeIf(token -> {
            Claims claims = jwtTokenService.validateToken(token);
            return claims == null; // Remove if token is invalid/expired
        });
        int removed = initialSize - blacklistedTokens.size();
        if (removed > 0) {
            logger.info("Cleaned up {} expired tokens from blacklist", removed);
        }
    }

    @Override
    public Set<String> getBlacklistedTokens() {
        return Set.copyOf(blacklistedTokens);
    }
}

