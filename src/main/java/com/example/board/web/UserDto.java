package com.example.board.web;

import com.example.board.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * 회원 관련 요청·응답.
 *
 * <p>필드 이름과 규칙은 기획서가 정한다 — D-200 · D-201 · A-200 · A-204 · A-205.
 * 여기서 마음대로 바꾸면 화면의 types.ts 와 갈라진다.
 */
public final class UserDto {

    private UserDto() {}

    /** D-200 회원. <b>자기 자신을 볼 때만</b> 이 모양이다 */
    public record Me(Long id, String email, String nickname, Instant createdAt) {
        public static Me of(User u) {
            return new Me(u.getId(), u.getEmail(), u.getNickname(), u.getCreatedAt());
        }
    }

    /** D-201 로그인 결과. 토큰만 주면 화면이 이름 하나 띄우려고 요청을 한 번 더 해야 한다 */
    public record LoginResult(String token, Me me) {}

    public record SignupRequest(
            @Email(message = "이메일 형식이 아니에요") @NotBlank(message = "이메일을 입력해 주세요")
            String email,
            @Size(min = 8, message = "비밀번호는 8자 이상이에요")
            String password,
            @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하예요")
            String nickname) {}

    public record LoginRequest(
            @NotBlank(message = "이메일을 입력해 주세요") String email,
            @NotBlank(message = "비밀번호를 입력해 주세요") String password) {}

    public record NicknameRequest(
            @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하예요") String nickname) {}

    public record PasswordRequest(
            @NotBlank(message = "현재 비밀번호를 입력해 주세요") String currentPassword,
            @Size(min = 8, message = "비밀번호는 8자 이상이에요") String newPassword) {}
}
