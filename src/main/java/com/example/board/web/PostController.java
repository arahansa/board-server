package com.example.board.web;

import com.example.board.domain.Post;
import com.example.board.domain.PostRepository;
import com.example.board.domain.User;
import com.example.board.domain.UserRepository;
import com.example.board.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** A-100 ~ A-104 — plan-docs/api/board/ */
@RestController
@RequestMapping("/posts")
public class PostController {

    /** 기본값을 서버가 든다. 화면이 매번 보내지 않아도 되고, curl 로 불러도 같은 결과가 나온다 */
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final PostRepository posts;
    private final UserRepository users;

    public PostController(PostRepository posts, UserRepository users) {
        this.posts = posts;
        this.users = users;
    }

    /** A-100 목록. 로그인 없이 읽힌다 */
    @GetMapping
    public PostDto.PageResult list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "LATEST") String sort) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_SIZE), // Math.clamp 은 Java 21 부터다
                // E-001 의 값 둘뿐이다. 모르는 값은 기본값으로 떨어뜨린다 — 400 을 주면
                // 주소에 남는 정렬 값 하나가 오타났을 때 목록이 통째로 안 뜬다
                "OLDEST".equals(sort)
                        ? Sort.by(Sort.Direction.ASC, "createdAt")
                        : Sort.by(Sort.Direction.DESC, "createdAt"));

        return PostDto.PageResult.of(
                q == null || q.isBlank() ? posts.findAll(pageable) : posts.search(q.trim(), pageable));
    }

    /** A-101 하나. 목록의 항목과 같은 모양이다 — 화면이 두 타입을 들고 다니지 않게 */
    @GetMapping("/{id}")
    public PostDto.Detail one(@PathVariable Long id) {
        return PostDto.Detail.of(posts.findById(id).orElseThrow(ApiException::notFound));
    }

    /** A-102 작성. 글쓴이를 본문으로 받지 않는다 — 받으면 남의 이름으로 쓸 수 있다 */
    @PostMapping
    @Transactional
    public PostDto.Detail create(
            @AuthenticationPrincipal AuthUser me, @Valid @RequestBody PostDto.CreateRequest req) {
        User author = users.findById(me.id()).orElseThrow(ApiException::notFound);
        return PostDto.Detail.of(
                posts.save(new Post(req.title().trim(), req.content(), author)));
    }

    /** A-103 수정 */
    @PatchMapping("/{id}")
    @Transactional
    public PostDto.Detail update(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable Long id,
            @Valid @RequestBody PostDto.UpdateRequest req) {

        Post post = mine(id, me);
        // 빈 문자열은 "지우기" 가 아니라 검증 실패다. null(안 보냄)과 나눈다
        if (req.title() != null && req.title().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "제목을 입력해 주세요");
        if (req.content() != null && req.content().isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "내용을 입력해 주세요");

        post.edit(req.title() == null ? null : req.title().trim(), req.content());
        return PostDto.Detail.of(post);
    }

    /** A-104 삭제. 진짜로 지운다 — 지운 사람이 지우려고 지운 것이다 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        posts.delete(mine(id, me));
    }

    /**
     * 내 글 하나. <b>없는 것과 남의 것을 똑같이 404 로 답한다.</b>
     *
     * <p>403 을 주면 "그 번호의 글은 있는데 네 것이 아니다" 가 샌다. 글 번호가 연번이라
     * 훑으면 누가 몇 개 썼는지 알 수 있게 된다 (A-103).
     */
    private Post mine(Long id, AuthUser me) {
        return posts.findById(id)
                .filter(p -> p.getAuthor().getId().equals(me.id()))
                .orElseThrow(ApiException::notFound);
    }
}
