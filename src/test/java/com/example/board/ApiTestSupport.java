package com.example.board;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.board.domain.PostRepository;
import com.example.board.domain.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP 로 시험한다. 서비스 메서드를 직접 부르지 않는 이유는 <b>기획서가 약속한 것이
 * HTTP 의 모양</b>이기 때문이다 — 상태 코드 · 필드 이름 · 문구. 그것들은 컨트롤러와
 * 직렬화와 Security 필터를 다 지나야 확정된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    private UserRepository users;

    @Autowired
    private PostRepository posts;

    @BeforeEach
    void 비운다() {
        // 글이 회원을 참조하므로 순서가 있다
        posts.deleteAll();
        users.deleteAll();
    }

    protected String body(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    protected JsonNode read(String raw) throws Exception {
        return json.readTree(raw);
    }

    /** 가입하고 토큰을 받는다. 가입이 곧 로그인이다 (A-200) */
    protected String signup(String email, String nickname) throws Exception {
        String res = mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","nickname":"%s"}
                                """.formatted(email, nickname)))
                .andReturn().getResponse().getContentAsString();
        return read(res).path("token").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
