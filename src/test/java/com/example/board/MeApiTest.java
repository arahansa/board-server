package com.example.board;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** A-203 ~ A-205 */
class MeApiTest extends ApiTestSupport {

    @Test
    @DisplayName("A-203 토큰이 없으면 401 이고 본문에 message 가 있다")
    void 토큰_없이_내정보() throws Exception {
        // 기본값은 빈 본문 401 이다. 그러면 화면(client.ts)이 읽을 것이 없다
        mvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요해요"));
    }

    @Test
    @DisplayName("A-204 닉네임을 바꾸면 이미 쓴 글의 글쓴이 이름도 함께 바뀐다")
    void 닉네임_수정은_옛_글에도_비친다() throws Exception {
        String token = signup("a@b.com", "옛이름");

        String post = mvc.perform(post("/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","content":"내용"}
                                """))
                .andReturn().getResponse().getContentAsString();
        long id = read(post).path("id").asLong();

        mvc.perform(patch("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"새이름"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새이름"));

        // Post 가 이름을 복사해 두지 않고 회원을 참조하기 때문이다 (D-101)
        mvc.perform(get("/posts/" + id))
                .andExpect(jsonPath("$.author.nickname").value("새이름"));
    }

    @Test
    @DisplayName("A-204 이메일은 바꿀 수 없다 — 보내도 무시된다")
    void 이메일은_안_바뀐다() throws Exception {
        String token = signup("a@b.com", "아라");

        mvc.perform(patch("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"아라2","email":"hacked@b.com"}
                                """))
                .andExpect(status().isOk())
                // DTO 에 email 이 아예 없어서 들어올 자리가 없다 (D-200)
                .andExpect(jsonPath("$.email").value("a@b.com"));
    }

    @Test
    @DisplayName("A-205 현재 비밀번호가 틀리면 401")
    void 비밀번호_변경_실패() throws Exception {
        String token = signup("a@b.com", "아라");

        mvc.perform(patch("/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrongpassword","newPassword":"newpassword123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("현재 비밀번호가 맞지 않아요"));
    }

    @Test
    @DisplayName("A-205 비밀번호를 바꾸면 지금 토큰도 죽고, 새 비밀번호로는 들어가진다")
    void 비밀번호_변경은_로그아웃시킨다() throws Exception {
        String token = signup("a@b.com", "아라");

        mvc.perform(patch("/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"newpassword123"}
                                """))
                .andExpect(status().isNoContent());

        // 바꾸는 이유가 '누가 내 계정을 쓰는 것 같다' 일 때, 그 사람의 토큰이 살아 있으면
        // 바꾼 의미가 없다 (A-205)
        mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"newpassword123"}
                                """))
                .andExpect(status().isOk());
    }
}
