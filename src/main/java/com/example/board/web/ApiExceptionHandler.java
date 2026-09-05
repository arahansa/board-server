package com.example.board.web;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 실패를 {@code {"message": "..."}} 한 모양으로 낸다. 화면(client.ts)이 이 키만 읽는다 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handle(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }

    /**
     * Bean Validation 실패. <b>첫 번째 것만 낸다</b> — 화면은 한 번에 한 줄만 보여 주고,
     * 전부 보내면 어느 칸이 문제인지 오히려 흐려진다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handle(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("입력을 다시 확인해 주세요");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    /**
     * 그 밖. 예외 문구를 그대로 내보내지 않는다 — 스택이나 SQL 이 섞여 나가면 내부 구조를
     * 알려 주는 꼴이 된다. 로그에는 남는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handle(Exception e) {
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "서버에 문제가 생겼어요"));
    }
}
