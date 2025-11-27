package com.example.demo.security;

import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.repository.RefreshTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final Key key;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtService(@Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.access-token-expiration-seconds}") long accessExpirationSeconds,
        @Value("${app.jwt.refresh-token-expiration-seconds}") long refreshExpirationSeconds,
        RefreshTokenRepository refreshTokenRepository) {
        if (secret == null || secret.length() < 32) {
            // fallback: generate a secure random key
            Key k = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            this.key = k;
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("name", user.getName())
            .claim("roles", user.getRoles())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(accessExpirationSeconds)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public RefreshToken generateAndStoreRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(refreshExpirationSeconds);
        RefreshToken rt = RefreshToken.builder()
            .token(token)
            .user(user)
            .expiryDate(expiry)
            .build();
        
        return refreshTokenRepository.save(rt);
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    public String getSubject(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
}
