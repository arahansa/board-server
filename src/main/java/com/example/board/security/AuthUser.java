package com.example.board.security;

/**
 * 인증된 사용자. 컨트롤러가 {@code @AuthenticationPrincipal AuthUser me} 로 받는다.
 *
 * <p>엔티티를 principal 로 두지 않는다 — 그러면 세션 없는 요청마다 영속성 컨텍스트 밖의
 * 엔티티가 돌아다니고, 실수로 그것을 저장하면 오래된 값이 덮인다. 필요한 것은 id 뿐이다.
 */
public record AuthUser(Long id) {}
