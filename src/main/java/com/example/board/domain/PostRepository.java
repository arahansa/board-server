package com.example.board.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 목록에 글쓴이 이름이 들어간다. EntityGraph 가 없으면 20건마다 쿼리가 20번 더 나간다 */
    @Override
    @EntityGraph(attributePaths = "author")
    Page<Post> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "author")
    Optional<Post> findById(Long id);

    /**
     * 제목과 본문을 함께 본다. 둘을 나누면 화면에 검색 대상 선택이 필요해지고,
     * 게시판 규모에서 그 선택은 쓰는 사람에게 부담만 준다 (A-100).
     */
    @EntityGraph(attributePaths = "author")
    @Query("""
            select p from Post p
            where lower(p.title) like lower(concat('%', :q, '%'))
               or lower(p.content) like lower(concat('%', :q, '%'))
            """)
    Page<Post> search(String q, Pageable pageable);
}
