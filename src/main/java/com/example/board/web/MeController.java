package com.example.board.web;

import com.example.board.domain.User;
import com.example.board.domain.UserRepository;
import com.example.board.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** A-203 · A-204 · A-205 — plan-docs/api/user/ */
@RestController
public class MeController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public MeController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** A-203 내 정보. 화면이 새로고침 뒤 로그인 상태를 되찾는 자리다 */
    @GetMapping("/me")
    public UserDto.Me me(@AuthenticationPrincipal AuthUser me) {
        return UserDto.Me.of(load(me));
    }

    /** A-204 닉네임 수정. 이메일은 못 바꾼다 (D-200) */
    @PatchMapping("/me")
    @Transactional
    public UserDto.Me rename(
            @AuthenticationPrincipal AuthUser me, @Valid @RequestBody UserDto.NicknameRequest req) {
        User user = load(me);
        user.rename(req.nickname().trim());
        return UserDto.Me.of(user);
    }

    /**
     * A-205 비밀번호 변경. 현재 비밀번호를 확인한다.
     *
     * <p>바꾸면 tokenVersion 이 올라 <b>지금 토큰도 죽는다.</b> 바꾸는 이유가 "누가 내 계정을
     * 쓰는 것 같다" 일 때, 바꿔도 그 사람의 토큰이 살아 있으면 바꾼 의미가 없다.
     */
    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void changePassword(
            @AuthenticationPrincipal AuthUser me, @Valid @RequestBody UserDto.PasswordRequest req) {
        User user = load(me);
        if (!encoder.matches(req.currentPassword(), user.getPassword()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 맞지 않아요");

        user.changePassword(encoder.encode(req.newPassword()));
    }

    private User load(AuthUser me) {
        return users.findById(me.id()).orElseThrow(ApiException::notFound);
    }
}
