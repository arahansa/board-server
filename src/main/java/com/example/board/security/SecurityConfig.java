package com.example.board.security;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 상태 없는 JWT 인증.
 *
 * <p>CORS 설정이 없다. 개발 중에는 vite 가 {@code /api} 를 프록시해 같은 출처가 되고,
 * 배포하면 같은 도메인 뒤에 선다 — <b>없는 설정은 틀릴 수도 없다.</b> 다른 출처에서
 * 부를 일이 생기면 그때 정확히 그 출처만 연다.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        return http
                // 토큰을 헤더로 싣는다. 쿠키가 아니므로 브라우저가 자동으로 붙이지 않고,
                // CSRF 가 성립하지 않는다
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 목록과 상세는 누구나 읽는다 (A-100 · A-101).
                        // 그래서 Post 의 글쓴이가 이메일을 들지 않는다 (D-101)
                        .requestMatchers(HttpMethod.GET, "/posts", "/posts/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login").permitAll()
                        .requestMatchers("/h2/**").permitAll()
                        .anyRequest().authenticated())
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // H2 콘솔이 frame 을 쓴다
                .exceptionHandling(e -> e.authenticationEntryPoint(SecurityConfig::unauthorized))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 인증 실패도 {@code {"message": ...}} 로 낸다.
     *
     * <p>기본값은 빈 본문 401 인데, 그러면 화면의 client.ts 가 읽을 것이 없어 상태 코드로만
     * 문구를 짐작하게 된다. 성공이든 실패든 응답 모양이 하나여야 화면이 단순해진다.
     */
    private static void unauthorized(
            jakarta.servlet.http.HttpServletRequest req,
            HttpServletResponse res,
            org.springframework.security.core.AuthenticationException e) throws java.io.IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"message\":\"로그인이 필요해요\"}");
    }
}
