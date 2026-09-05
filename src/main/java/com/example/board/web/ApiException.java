package com.example.board.web;

import org.springframework.http.HttpStatus;

/**
 * 화면에 그대로 보일 문구를 든 실패.
 *
 * <p>문구를 서버가 든다. 화면마다 제 말로 옮기면 기획서의 전환 표와 갈라지고,
 * 그때 어느 쪽이 맞는지 확인할 방법이 없다.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 없거나 · 내 것이 아닌 것. <b>403 을 쓰지 않는다</b> — 403 은 "그 번호의 글은 있는데
     * 네 것이 아니다" 를 흘린다. 글 번호가 연번이라 훑으면 누가 몇 개 썼는지 알 수 있게 된다.
     */
    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "찾는 것이 없어요");
    }
}
