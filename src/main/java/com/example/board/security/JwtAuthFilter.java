package com.example.board.security;

import com.example.board.domain.User;
import com.example.board.domain.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer <jwt>} 를 읽어 인증을 세운다.
 *
 * <p>토큰이 없거나 틀렸으면 <b>아무것도 하지 않고 통과시킨다.</b> 여기서 401 을 던지면
 * 로그인 없이 읽어야 하는 목록·상세까지 막힌다 — 거절은 SecurityConfig 가 경로별로 정한다.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        authenticate(req.getHeader("Authorization"))
                .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
        chain.doFilter(req, res);
    }

    private Optional<UsernamePasswordAuthenticationToken> authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();

        Claims claims = jwt.read(header.substring(7));
        if (claims == null) return Optional.empty();

        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        return users.findById(userId)
                // 세대가 다르면 로그아웃했거나 비밀번호를 바꾼 것이다. 서명이 맞아도 거절한다 —
                // 이 한 줄이 없으면 로그아웃이 아무것도 하지 못한다 (A-202)
                .filter(u -> u.getTokenVersion() == claims.get(JwtService.CLAIM_TOKEN_VERSION, Integer.class))
                .map(this::toAuthentication);
    }

    private UsernamePasswordAuthenticationToken toAuthentication(User u) {
        return new UsernamePasswordAuthenticationToken(new AuthUser(u.getId()), null, List.of());
    }
}
