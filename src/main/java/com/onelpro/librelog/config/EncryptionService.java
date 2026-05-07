package com.onelpro.librelog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES/GCM encryption for at-rest secrets (e.g. LibreTime API keys).
 * Key is derived from {@code librelog.encryption.key} via SHA-256 to a 32-byte AES key.
 * Output format: base64(iv || ciphertext+tag).
 */
@Service
@RequiredArgsConstructor
public class EncryptionService {

    private static final int IV_LEN = 12;
    private static final int TAG_LEN_BITS = 128;

    private final AppProperties props;

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LEN_BITS, iv));
            byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String ciphertextB64) {
        if (ciphertextB64 == null) return null;
        try {
            byte[] in = Base64.getDecoder().decode(ciphertextB64);
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(in, 0, iv, 0, IV_LEN);
            byte[] ct = new byte[in.length - IV_LEN];
            System.arraycopy(in, IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LEN_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private SecretKeySpec key() throws Exception {
        byte[] raw = MessageDigest.getInstance("SHA-256")
                .digest(props.getEncryption().getKey().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(raw, "AES");
    }
}
