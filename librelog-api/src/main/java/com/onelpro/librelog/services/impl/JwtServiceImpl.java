package com.onelpro.librelog.services.impl;

import com.onelpro.librelog.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Implementation of JWT service for token generation and validation.
 */
@Service
public class JwtServiceImpl implements JwtService {

	private static final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.expiration:86400000}")
	private long expiration;

	@Override
	public String generateToken(UUID userId, String email, String role) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expiration);

		return Jwts.builder()
				.subject(userId.toString())
				.claim("email", email)
				.claim("role", role)
				.issuedAt(now)
				.expiration(expiryDate)
				.signWith(getSigningKey())
				.compact();
	}

	@Override
	public String extractUserId(String token) {
		Claims claims = extractAllClaims(token);
		return claims.getSubject();
	}

	@Override
	public String extractEmail(String token) {
		Claims claims = extractAllClaims(token);
		return claims.get("email", String.class);
	}

	@Override
	public String extractRole(String token) {
		Claims claims = extractAllClaims(token);
		return claims.get("role", String.class);
	}

	@Override
	public boolean isTokenValid(String token) {
		try {
			Claims claims = extractAllClaims(token);
			Date expiration = claims.getExpiration();
			return expiration.after(new Date());
		} catch (Exception e) {
			logger.warn("Token validation failed: {}", e.getMessage());
			return false;
		}
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

}

