package com.onelpro.librelog.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for encrypting and decrypting sensitive data.
 * Uses AES encryption with a secret key.
 */
public class EncryptionUtils {

	private static final Logger logger = LoggerFactory.getLogger(EncryptionUtils.class);
	private static final String ALGORITHM = "AES";
	private static final int KEY_SIZE = 256;

	// In production, this should be stored securely (e.g., environment variable, key management service)
	// For now, using a default key - MUST be changed in production
	private static final String ENCRYPTION_KEY = getEncryptionKey();

	private static String getEncryptionKey() {
		String key = System.getenv("LIBRELOG_ENCRYPTION_KEY");
		if (key != null && !key.isEmpty()) {
			return key;
		}
		// Default key for development - MUST be changed in production
		logger.warn("Using default encryption key. Set LIBRELOG_ENCRYPTION_KEY environment variable in production!");
		return "default-encryption-key-32-chars!!"; // Must be exactly 32 characters for AES-256
	}

	private static SecretKey getSecretKey() {
		byte[] keyBytes = ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8);
		// Ensure key is exactly 32 bytes for AES-256
		byte[] key = new byte[32];
		System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
		return new SecretKeySpec(key, ALGORITHM);
	}

	/**
	 * Encrypts a plain text string.
	 * 
	 * @param plainText The text to encrypt
	 * @return Encrypted text (Base64 encoded), or null if input is null
	 */
	public static String encrypt(String plainText) {
		if (plainText == null || plainText.isEmpty()) {
			return plainText;
		}
		try {
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
			byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encryptedBytes);
		} catch (Exception e) {
			logger.error("Failed to encrypt data: {}", e.getMessage());
			throw new RuntimeException("Encryption failed", e);
		}
	}

	/**
	 * Decrypts an encrypted string.
	 * 
	 * @param encryptedText The encrypted text (Base64 encoded) to decrypt
	 * @return Decrypted text, or null if input is null
	 */
	public static String decrypt(String encryptedText) {
		if (encryptedText == null || encryptedText.isEmpty()) {
			return encryptedText;
		}
		try {
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
			byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
			return new String(decryptedBytes, StandardCharsets.UTF_8);
		} catch (Exception e) {
			logger.error("Failed to decrypt data: {}", e.getMessage());
			throw new RuntimeException("Decryption failed", e);
		}
	}

	/**
	 * Masks sensitive data for display (shows only last 4 characters).
	 * 
	 * @param data The data to mask
	 * @return Masked data (e.g., "****1234")
	 */
	public static String maskForDisplay(String data) {
		if (data == null || data.isEmpty()) {
			return "";
		}
		if (data.length() <= 4) {
			return "****";
		}
		return "****" + data.substring(data.length() - 4);
	}

}

