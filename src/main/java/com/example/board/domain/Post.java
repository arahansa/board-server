package com.example.board.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** 게시글. 기획서 D-100 (plan-docs/domain/board/d-post.md) */
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    /** 이름을 복사해 두지 않는다. 닉네임을 고치면 옛 글의 글쓴이도 함께 바뀐다 (A-204) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * 마지막 수정 시각. <b>고친 적 없으면 null 이다.</b>
     *
     * <p>createdAt 을 복사해 넣지 않는다. 그러면 화면이 "수정됨" 을 판정할 근거를 잃는다 (D-100).
     */
    private Instant updatedAt;

    protected Post() {}

    public Post(String title, String content, User author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public User getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** PATCH 는 부분 갱신이다 — null 로 온 필드는 건드리지 않는다 (A-103) */
    public void edit(String title, String content) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        this.updatedAt = Instant.now();
    }
}
