package com.wasel.backend.security;

import com.wasel.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String generateAccessToken(UserAccount user) {
        return buildToken(user, TokenType.ACCESS, Instant.now().plus(jwtProperties.accessTokenMinutes(), ChronoUnit.MINUTES));
    }

    public String generateRefreshToken(UserAccount user, String tokenId) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole().name(),
                        "type", TokenType.REFRESH.name(),
                        "jti", tokenId
                ))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(jwtProperties.refreshTokenDays(), ChronoUnit.DAYS)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public TokenType extractTokenType(String token) {
        return TokenType.valueOf(parse(token).get("type", String.class));
    }

    public String extractRefreshTokenId(String token) {
        return parse(token).get("jti", String.class);
    }

    private String buildToken(UserAccount user, TokenType type, Instant expiresAt) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole().name(),
                        "type", type.name()
                ))
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }
}
