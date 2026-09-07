package com.pipeline.crm.security;

import com.pipeline.crm.config.JwtProperties;
import com.pipeline.crm.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String username, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role.name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.accessTtlMinutes() * 60)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.refreshTtlDays() * 24 * 3600)))
                .signWith(key)
                .compact();
    }

    public TokenPayload parse(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String type = claims.get("type", String.class);
        if (!expectedType.equals(type)) {
            throw new RuntimeException("Unexpected token type");
        }
        return new TokenPayload(
                UUID.fromString(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", String.class));
    }

    public record TokenPayload(UUID userId, String username, String role) {
    }
}
