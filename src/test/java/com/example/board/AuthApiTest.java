package com.example.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** A-200 ~ A-205. 기획서의 `오류` 행에 적은 코드가 실제로 그 코드인지 본다 */
class AuthApiTest extends ApiTestSupport {

    @Test
    @DisplayName("A-200 가입하면 토큰과 본인 정보를 함께 준다")
    void 가입() throws Exception {
        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"password123","nickname":"아라"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.me.email").value("a@b.com"))
                .andExpect(jsonPath("$.me.nickname").value("아라"))
                // 비밀번호는 어떤 응답에도 실리지 않는다
                .andExpect(jsonPath("$.me.password").doesNotExist());
    }

    @Test
    @DisplayName("A-200 이메일 중복은 400 이 아니라 409 다")
    void 이메일_중복() throws Exception {
        signup("a@b.com", "아라");

        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"password123","nickname":"다른사람"}
                                """))
                // 400 으로 뭉개면 화면이 '형식이 틀렸다' 와 '이미 있다' 를 구분하지 못한다
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일이에요"));
    }

    @Test
    @DisplayName("A-200 비밀번호가 8자 미만이면 400 이고 문구는 기획서의 것이다")
    void 가입_검증() throws Exception {
        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"short","nickname":"아라"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상이에요"));
    }

    @Test
    @DisplayName("A-201 없는 이메일과 틀린 비밀번호가 같은 401·같은 문구다")
    void 로그인_실패는_구분되지_않는다() throws Exception {
        signup("a@b.com", "아라");

        String 없는이메일 = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@b.com","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String 틀린비밀번호 = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // 나누면 이메일 존재 여부를 물어보는 도구가 된다 (A-201)
        assertThat(없는이메일).isEqualTo(틀린비밀번호);
    }

    @Test
    @DisplayName("A-202 로그아웃하면 그 토큰이 실제로 죽는다")
    void 로그아웃은_토큰을_끊는다() throws Exception {
        String token = signup("a@b.com", "아라");

        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        // 서명은 그대로 맞다. tokenVersion 이 달라져서 거절된다 —
        // 이 줄이 실패하면 로그아웃이 아무것도 하지 않는 것이다
        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }
}
