package com.example.board.web;

import com.example.board.domain.User;
import com.example.board.domain.UserRepository;
import com.example.board.security.AuthUser;
import com.example.board.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** A-200 · A-201 · A-202 — plan-docs/api/user/ */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** A-200 회원가입. 가입하면 바로 로그인된다 — 방금 넣은 것을 또 넣으라고 하지 않는다 */
    @PostMapping("/signup")
    @Transactional
    public UserDto.LoginResult signup(@Valid @RequestBody UserDto.SignupRequest req) {
        // 409 다. 400 으로 뭉개면 화면이 "형식이 틀렸다" 와 "이미 있다" 를 구분하지 못한다
        if (users.existsByEmail(req.email()))
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일이에요");

        User saved = users.save(
                new User(req.email(), encoder.encode(req.password()), req.nickname().trim()));
        return new UserDto.LoginResult(jwt.issue(saved), UserDto.Me.of(saved));
    }

    /** A-201 로그인 */
    @PostMapping("/login")
    public UserDto.LoginResult login(@Valid @RequestBody UserDto.LoginRequest req) {
        User user = users.findByEmail(req.email())
                .filter(u -> encoder.matches(req.password(), u.getPassword()))
                // 무엇이 틀렸는지 말하지 않는다. 나누면 이메일 존재 여부를 물어보는 도구가 된다
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "이메일이나 비밀번호가 맞지 않아요"));

        return new UserDto.LoginResult(jwt.issue(user), UserDto.Me.of(user));
    }

    /**
     * A-202 로그아웃. tokenVersion 을 올려 <b>서버가 실제로 끊는다.</b>
     *
     * <p>화면에서 토큰만 버리면 그 토큰은 유효기간까지 살아 있다. 기기 전체가 함께
     * 로그아웃되는 것은 이 방식의 성질이고, 숨기지 않는 편이 정직하다.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void logout(@AuthenticationPrincipal AuthUser me) {
        users.findById(me.id()).orElseThrow(ApiException::notFound).logoutEverywhere();
    }
}
