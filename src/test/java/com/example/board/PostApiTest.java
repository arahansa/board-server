package com.example.board;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** A-100 ~ A-104 */
class PostApiTest extends ApiTestSupport {

    private long write(String token, String title, String content) throws Exception {
        String res = mvc.perform(post("/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of("title", title, "content", content))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return read(res).path("id").asLong();
    }

    @Test
    @DisplayName("A-100 목록은 로그인 없이 읽히고, 글쓴이에 이메일이 없다")
    void 목록은_공개다() throws Exception {
        String token = signup("a@b.com", "아라");
        write(token, "첫 글", "내용");

        mvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].author.nickname").value("아라"))
                // 목록은 누구나 읽는다. 이메일을 실으면 아무나 긁어 간다 (D-101)
                .andExpect(jsonPath("$.items[0].author.email").doesNotExist());
    }

    @Test
    @DisplayName("A-100 기본값은 서버가 정한다 — page=0 · size=20 · LATEST")
    void 기본값() throws Exception {
        String token = signup("a@b.com", "아라");
        write(token, "먼저", "내용");
        write(token, "나중", "내용");

        mvc.perform(get("/posts"))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].title").value("나중"));

        mvc.perform(get("/posts").param("sort", "OLDEST"))
                .andExpect(jsonPath("$.items[0].title").value("먼저"));

        // 모르는 정렬 값은 기본값으로 떨어진다. 400 을 주면 주소에 남는 값 하나가
        // 오타났을 때 목록이 통째로 안 뜬다 (E-001)
        mvc.perform(get("/posts").param("sort", "WHATEVER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("나중"));
    }

    @Test
    @DisplayName("A-100 q 는 제목과 본문을 함께 본다")
    void 검색() throws Exception {
        String token = signup("a@b.com", "아라");
        write(token, "제목에만 사과", "다른 내용");
        write(token, "다른 제목", "본문에만 사과");
        write(token, "관계없음", "관계없음");

        mvc.perform(get("/posts").param("q", "사과"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("A-102 로그인 없이 쓰면 401")
    void 작성은_로그인이_필요하다() throws Exception {
        mvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","content":"내용"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A-100·A-102 갓 쓴 글의 updatedAt 은 null 이다")
    void 수정한_적_없으면_updatedAt_은_null() throws Exception {
        String token = signup("a@b.com", "아라");
        long id = write(token, "제목", "내용");

        // createdAt 을 복사해 넣으면 화면이 '수정됨' 을 판정할 근거를 잃는다 (D-100)
        mvc.perform(get("/posts/" + id))
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        mvc.perform(patch("/posts/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"고친 제목"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("고친 제목"))
                // 보내지 않은 필드는 그대로다 (A-103)
                .andExpect(jsonPath("$.content").value("내용"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("A-103·A-104 남의 글은 403 이 아니라 404 다")
    void 남의_글은_404() throws Exception {
        String 주인 = signup("a@b.com", "주인");
        String 남 = signup("c@d.com", "남남");  // 닉네임은 2자 이상이다 (A-200)
        long id = write(주인, "제목", "내용");

        // 403 을 주면 '그 번호의 글은 있는데 네 것이 아니다' 가 샌다.
        // 글 번호가 연번이라 훑으면 누가 몇 개 썼는지 알 수 있게 된다 (A-103)
        mvc.perform(patch("/posts/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(남))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"가로채기"}
                                """))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/posts/" + id).header(HttpHeaders.AUTHORIZATION, bearer(남)))
                .andExpect(status().isNotFound());

        // 없는 글도 같은 404 다 — 두 경우를 구분할 수 없어야 한다
        mvc.perform(delete("/posts/999999").header(HttpHeaders.AUTHORIZATION, bearer(남)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A-104 내 글은 진짜로 지워진다")
    void 삭제() throws Exception {
        String token = signup("a@b.com", "아라");
        long id = write(token, "제목", "내용");

        mvc.perform(delete("/posts/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        // soft delete 가 아니다. 지운 사람이 지우려고 지운 것이다 (A-104)
        mvc.perform(get("/posts/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/posts")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("A-102 제목이 100자를 넘으면 400 이고 문구는 기획서의 것이다")
    void 검증() throws Exception {
        String token = signup("a@b.com", "아라");

        mvc.perform(post("/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of("title", "가".repeat(101), "content", "내용"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("제목은 100자까지예요"));

        mvc.perform(post("/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of("title", "  ", "content", "내용"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("제목을 입력해 주세요"));
    }
}
