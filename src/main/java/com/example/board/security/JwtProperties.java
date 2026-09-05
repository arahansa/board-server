package com.example.board.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 {@code board.jwt}. secret 이 새면 토큰을 위조할 수 있다 */
@ConfigurationProperties(prefix = "board.jwt")
public record JwtProperties(String secret, long ttlHours) {}
