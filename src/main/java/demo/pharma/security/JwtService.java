package demo.pharma.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration}") long expiration) {
        if (secret == null || secret.length() < 32)
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generate(AppUser u) {
        Instant now = Instant.now();
        return Jwts.builder().subject(u.getUsername()).claim("userId", u.getId().toString())
                .claim("roles", u.getRoles().stream().map(Role::getName).toList()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiration))).signWith(key).compact();
    }

    public String username(String token) {
        return claims(token).getSubject();
    }

    public boolean valid(String token, UserDetails user) {
        try {
            return username(token).equals(user.getUsername()) && claims(token).getExpiration().after(new Date());
        } catch (JwtException e) {
            return false;
        }
    }

    public long expiration() {
        return expiration;
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
