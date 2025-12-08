package com.stary.backend.api.users;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@Getter
public class TokenManager {
    private final Key key;
    private final long jwtExpirationMs;
    private final long jwtRefreshExpirationMs;

    public TokenManager(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long jwtExpirationMs,
                        @Value("${jwt.refresh-expiration-ms}") long jwtRefreshExpirationMs){
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtRefreshExpirationMs = jwtRefreshExpirationMs;
    }

    public String generateAccessToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }
}
