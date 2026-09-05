package com.example.board.security;

import com.example.board.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

/** JWT 를 만들고 읽는다. 클레임은 둘뿐이다 — 누구인가(sub)와 몇 세대인가(tv) */
@Service
public class JwtService {

    /** 토큰 세대. 로그아웃·비밀번호 변경이 올린다 (A-202 · A-205) */
    static final String CLAIM_TOKEN_VERSION = "tv";

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(props.ttlHours());
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** 서명·유효기간이 맞으면 클레임, 아니면 null. 실패 이유를 나누지 않는다 — 부르는 쪽이 401 하나로 답한다 */
    public Claims read(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
