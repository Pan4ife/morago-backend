package org.morago.security;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.morago.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("type", "access")
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
                .claim("type", "refresh")
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

    public String extractTokenType(String token) {

        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("type", String.class);
    }
}
