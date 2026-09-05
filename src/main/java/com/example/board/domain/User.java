package com.example.board.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** 회원. 기획서 D-200 (plan-docs/domain/user/d-me.md) */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 아이디를 겸한다. 바꿀 수 없다 (D-200) */
    @Column(nullable = false, unique = true, length = 200)
    private String email;

    /** BCrypt 해시. 평문은 어디에도 남지 않는다 */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    /**
     * 발급한 토큰의 세대. 로그아웃과 비밀번호 변경이 이 값을 올린다.
     *
     * <p>JWT 는 상태가 없어 "폐기" 가 원래 없다. 화면에서 토큰만 버리면 그 토큰은 유효기간까지
     * 살아 있고, 새어 나갔을 때 로그아웃이 아무것도 하지 못한다. 클레임에 이 값을 실어 두고
     * 지금 값과 다르면 거절하면 실제로 끊긴다 (A-202).
     */
    @Column(nullable = false)
    private int tokenVersion = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {}

    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    public int getTokenVersion() { return tokenVersion; }
    public Instant getCreatedAt() { return createdAt; }

    public void rename(String nickname) { this.nickname = nickname; }

    /** 비밀번호를 바꾸면 지금 토큰도 함께 죽는다 — 바꾸는 이유가 대개 그것이다 (A-205) */
    public void changePassword(String encoded) {
        this.password = encoded;
        this.tokenVersion++;
    }

    public void logoutEverywhere() { this.tokenVersion++; }
}
