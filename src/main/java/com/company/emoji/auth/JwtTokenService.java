package com.company.emoji.auth;

import com.company.emoji.common.config.AuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtTokenService {
    private final AuthProperties authProperties;

    public JwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String issueAccessToken(String subject, Map<String, Object> claims) {
        return issueToken(subject, claims, authProperties.accessTokenTtlSeconds(), "access");
    }

    public String issueRefreshToken(String subject, Map<String, Object> claims) {
        return issueToken(subject, claims, authProperties.refreshTokenTtlSeconds(), "refresh");
    }

    private String issueToken(String subject, Map<String, Object> claims, long ttlSeconds, String tokenType) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .claim("tokenType", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }
}