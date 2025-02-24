package com.lime.token.provider;

import com.lime.token.domain.JwtTokenType;
import com.lime.token.provider.functions.TokenResolver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class JwtTokenProvider implements TokenProvider {

    private final String secret;
    private final Long accessTokenExpiration;
    private final Long refreshTokenExpiration;

    public JwtTokenProvider(String secret, final Long accessTokenExpiration, final Long refreshTokenExpiration) {
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;

        log.info("Initialized TokenProvider");
    }
    public JwtTokenProvider(TokenProperties tokenProperties) {
        this(tokenProperties.getSecret(), tokenProperties.getAccessTokenExpiration(), tokenProperties.getRefreshTokenExpiration());
    }

    public JwtTokenProvider(TokenProviderConfigurer configurer) {
        this(configurer.properties());
    }


    @Override
    public String createToken(Map<String, Object> claims) {
        TokenUtils.isValidClaims(claims);
        return Jwts.builder()
                .signWith(TokenUtils.getSecretKey(secret))
                .claims(claims)
                .compact();

    }

    @Override
    public String createToken(Map<String, Object> claims, JwtTokenType type) {
        LocalDateTime now = LocalDateTime.now();
        List<String> test = new ArrayList<>();

        return switch (type) {
            case ACCESS -> createToken(claims, now.plus(accessTokenExpiration, ChronoUnit.MILLIS));
            case REFRESH -> createToken(claims, now.plus(refreshTokenExpiration, ChronoUnit.MILLIS));
        };
    }

    @Override
    public String createToken(Map<String, Object> claims, LocalDateTime expiration) {
        Instant exp = ZonedDateTime.of(expiration, ZoneId.systemDefault()).toInstant();
        return Jwts.builder()
                .signWith(TokenUtils.getSecretKey(secret))
                .claims(claims)
                .expiration(Date.from(exp))
                .compact();
    }

    @Override
    public String resolvedToken(TokenResolver<HttpServletRequest, String> resolver, ServletRequest request) {
        return resolver.apply((HttpServletRequest) request);
    }


    @Override
    @SuppressWarnings("rawtypes")
    public Map parsePayload(String token) {
        return TokenUtils.decodePayload(token);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public boolean isExpiredToken(String token) {
        Map<String, Object> map = TokenUtils.decodePayload(token);

        LocalDateTime now = LocalDateTime.now();
        Object exp = map.get("exp");
        if(map.isEmpty() || exp == null) {
            return true;
        }
        LocalDateTime expired = TokenUtils.toLocalDateTime(exp);
        return now.isAfter(expired);
    }

    @Override
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(TokenUtils.getSecretKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}
