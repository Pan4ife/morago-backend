package org.morago.security;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.morago.model.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET = "5f2c3d8a1e7b9f6a4c8d2e5f7a1b3c9d5e6f8a2b4c7d9e1f3a5b7c9d2e4f6a8";

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + 1000 * 60 * 15)
                )
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {

        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token, User user) {

        String username = extractUsername(token);

        return username.equals(user.getEmail());
    }
}
