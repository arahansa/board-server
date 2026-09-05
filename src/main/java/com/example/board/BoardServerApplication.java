package com.example.board;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 게시판 예제 서버.
 *
 * <p>이 서버는 기획서(board-front 의 {@code plan-docs/})를 진실로 삼는다. 새 API 를 여기서
 * 먼저 만들지 않는다 — 문서를 먼저 쓰고, 그러면 {@code PlanDocsReconcileTest} 가
 * "문서에 있는데 라우트가 없다" 로 빨개진다. 그 순서를 뒤집으면 문서가 코드를 뒤따라가고,
 * 그때부터 아무도 문서를 믿지 않는다.
 */
@SpringBootApplication
public class BoardServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BoardServerApplication.class, args);
    }
}
