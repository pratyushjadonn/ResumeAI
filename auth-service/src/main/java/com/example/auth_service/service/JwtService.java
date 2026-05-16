package com.example.auth_service.service;

import com.example.auth_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final int MINIMUM_JWT_KEY_BYTES = 32;

    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    @Value("${security.jwt.access-token-expiration-ms:3600000}")
    private long accessTokenExpirationMs;

    @Value("${security.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @PostConstruct
    void validateConfiguration() {
        byte[] keyBytes = getSigningKeyBytes();
        if (keyBytes.length < MINIMUM_JWT_KEY_BYTES) {
            throw new IllegalStateException("security.jwt.secret must decode to at least 32 bytes");
        }
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("name", user.getFullName());
        return buildToken(claims, user.getEmail(), accessTokenExpirationMs);
    }

    public String generateRefreshToken(User user) {
        return buildToken(Map.of("type", "refresh"), user.getEmail(), refreshTokenExpirationMs);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractAllClaims(token).get("type", String.class));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null
                && username.equalsIgnoreCase(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(getSigningKeyBytes());
    }

    private byte[] getSigningKeyBytes() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret must be configured");
        }
        try {
            return Decoders.BASE64.decode(jwtSecret.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("security.jwt.secret must be valid Base64", ex);
        }
    }
}
