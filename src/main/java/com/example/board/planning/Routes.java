package com.example.board.planning;

import java.util.Set;
import java.util.TreeSet;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 이 서버가 실제로 여는 라우트.
 *
 * <p>컨트롤러 소스를 읽지 않고 {@link RequestMappingHandlerMapping} 에 묻는다. 소스를 긁으면
 * 실제로 열리는 것과 어긋날 수 있고 — {@code @RequestMapping} 이 클래스에도 붙으니 —
 * 대조가 거짓으로 통과한다.
 */
public final class Routes {

    /**
     * 프레임워크가 스스로 여는 것들. 기획서에 있을 리 없고, 없다고 실패시키면
     * 대조 테스트가 늘 빨간 채로 남아 아무도 안 본다.
     */
    private static final Set<String> IGNORED_PREFIX = Set.of("/error", "/h2");

    private Routes() {}

    /** {@code "GET /posts"} 꼴. context-path(/api)는 여기 붙지 않는다 — 기획서와 같은 모양이다 */
    public static Set<String> of(RequestMappingHandlerMapping mapping) {
        Set<String> out = new TreeSet<>();

        for (RequestMappingInfo info : mapping.getHandlerMethods().keySet()) {
            Set<String> patterns = info.getPathPatternsCondition() == null
                    ? Set.of()
                    : info.getPathPatternsCondition().getPatternValues();

            for (String path : patterns) {
                if (IGNORED_PREFIX.stream().anyMatch(path::startsWith)) continue;
                for (var method : info.getMethodsCondition().getMethods())
                    out.add(method.name() + " " + normalize(path));
            }
        }
        return out;
    }

    /**
     * 경로 변수 이름을 지우지 않는다 — 기획서도 {@code /posts/{id}} 로 적는다.
     * 끝의 {@code /} 만 떼어 {@code /posts} 와 {@code /posts/} 가 다른 것으로 보이지 않게 한다.
     */
    private static String normalize(String path) {
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
