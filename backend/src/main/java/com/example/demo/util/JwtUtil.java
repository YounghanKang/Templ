package com.example.demo.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtUtil(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        if (secret == null) secret = "";

        // Warn if using the default or an obviously weak secret
        if ("change-me-to-a-secure-value".equals(secret) || secret.trim().isEmpty()) {
            logger.warn("JWT secret is not set or uses the default placeholder. In production, set a strong secret via the JWT_SECRET environment variable (32+ random bytes). Using a weak default is unsafe.");
        } else if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            logger.warn("JWT secret is shorter than 32 bytes; a derived key will be used. For production, provide a 32+ byte secret via the JWT_SECRET environment variable to avoid weaker derived keys.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 requires a sufficiently long secret; derive 32-byte key via SHA-256 when input is short
        if (keyBytes.length < 32) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                keyBytes = md.digest(keyBytes);
                logger.info("Derived a 32-byte HMAC key from configured JWT secret (SHA-256) for runtime compatibility.");
            } catch (NoSuchAlgorithmException e) {
                // fallback to using raw bytes (will likely fail later but preserve behavior)
                logger.error("SHA-256 not available, falling back to zero-padded secret bytes. This is unsafe.", e);
                keyBytes = new byte[32];
                byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
                System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 32));
            }
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationSeconds * 1000);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate the token and return the subject (username) if valid, otherwise return null.
     */
    public String validateTokenAndGetSubject(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }
}
