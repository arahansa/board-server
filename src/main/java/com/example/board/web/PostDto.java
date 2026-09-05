package com.example.board.web;

import com.example.board.domain.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

/** 게시판 요청·응답. 기획서 D-100 · D-101 · D-102 · A-102 · A-103 */
public final class PostDto {

    private PostDto() {}

    /** D-101 글쓴이. <b>이메일이 오지 않는다</b> — 목록은 로그인 없이 읽히기 때문이다 */
    public record Author(Long id, String nickname) {}

    /** D-100 게시글 */
    public record Detail(
            Long id,
            String title,
            String content,
            Author author,
            Instant createdAt,
            Instant updatedAt) {

        public static Detail of(Post p) {
            return new Detail(
                    p.getId(),
                    p.getTitle(),
                    p.getContent(),
                    new Author(p.getAuthor().getId(), p.getAuthor().getNickname()),
                    p.getCreatedAt(),
                    // 고친 적 없으면 null 이다. createdAt 을 복사해 넣지 않는다 (D-100)
                    p.getUpdatedAt());
        }
    }

    /** D-102 목록 봉투. <b>모든 목록 API 가 같은 모양을 쓴다</b> */
    public record PageResult(
            List<Detail> items, int page, int size, long totalElements, int totalPages) {

        public static PageResult of(Page<Post> page) {
            return new PageResult(
                    page.getContent().stream().map(Detail::of).toList(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages());
        }
    }

    public record CreateRequest(
            @NotBlank(message = "제목을 입력해 주세요") @Size(max = 100, message = "제목은 100자까지예요")
            String title,
            @NotBlank(message = "내용을 입력해 주세요") @Size(max = 5000, message = "내용은 5000자까지예요")
            String content) {}

    /** PATCH 는 부분 갱신이다 — 보내지 않은 필드는 null 로 와서 그대로 남는다 (A-103) */
    public record UpdateRequest(
            @Size(max = 100, message = "제목은 100자까지예요") String title,
            @Size(max = 5000, message = "내용은 5000자까지예요") String content) {}
}
