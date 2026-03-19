package com.library.system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createToken(LoginUser loginUser) {
        Instant now = Instant.now();
        Instant expiredAt = now.plusSeconds(jwtProperties.getExpirationSeconds());

        return Jwts.builder()
                .subject(loginUser.getUsername())
                .claim("uid", loginUser.getId())
                .claim("role", loginUser.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiredAt))
                .signWith(getKey())
                .compact();
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        Object uid = claims.get("uid");
        if (uid instanceof Integer integer) {
            return integer.longValue();
        }
        if (uid instanceof Long l) {
            return l;
        }
        return Long.valueOf(String.valueOf(uid));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        String secret = jwtProperties.getSecret() == null ? "" : jwtProperties.getSecret().trim();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // HS256 要求至少 256 bit（32 字节）密钥，过短时做 SHA-256 扩展，避免运行时异常。
        if (keyBytes.length < 32) {
            try {
                keyBytes = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            } catch (Exception ex) {
                throw new IllegalStateException("JWT 密钥处理失败", ex);
            }
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
