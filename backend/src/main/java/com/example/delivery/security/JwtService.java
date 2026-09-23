package com.example.delivery.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/** Creates and validates short-lived signed access tokens. */
@Service
public class JwtService {
    private static final String TOKEN_TYPE = "access";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.expiration-minutes:120}") long expirationMinutes) {
        this.key = buildKey(secret);
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(Long userId, String username, String role) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("uid", userId)
                .claim("role", role)
                .claim("typ", TOKEN_TYPE)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public UserPrincipal parseToken(String token) throws JwtException, IllegalArgumentException {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!TOKEN_TYPE.equals(claims.get("typ", String.class))) {
            throw new JwtException("Unsupported token type");
        }
        Object rawUserId = claims.get("uid");
        if (!(rawUserId instanceof Number userId)) {
            throw new JwtException("Token user id is missing");
        }
        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        if (username == null || role == null) {
            throw new JwtException("Token identity is incomplete");
        }
        return new UserPrincipal(userId.longValue(), username, role);
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("security.jwt.secret must be configured in .env or JWT_SECRET");
        }
        byte[] secretBytes = decodeSecret(secret);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("security.jwt.secret must contain at least 256 bits of key material");
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }

    private static byte[] decodeSecret(String secret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            return decoded.length >= 32 ? decoded : secret.getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
