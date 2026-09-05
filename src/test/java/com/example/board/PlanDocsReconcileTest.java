package com.example.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.board.planning.PlanDocs;
import com.example.board.planning.Routes;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * ★ 이 예제의 핵심.
 *
 * <p>기획서(board-front 의 {@code plan-docs/api/**})와 이 서버가 실제로 여는 라우트를
 * 맞춰 본다. 어긋나면 빌드가 선다.
 *
 * <p><b>왜 테스트인가.</b> 문서와 코드가 갈라지는 것은 조용히 일어난다 — 기획서에서 API 를
 * 뺐는데 서버는 계속 그것을 열어 두거나, 문서를 먼저 썼는데 아무도 만들지 않거나. 사람이
 * 눈으로 대조하면 몇 번은 하다가 안 하게 된다. 빌드가 서면 안 할 수가 없다.
 *
 * <p><b>왜 api-status 를 여기서 안 보나.</b> 네트워크가 필요한 것을 빌드에 묶으면 토큰 없이는
 * 아무것도 못 하게 된다. 상태(st_*)와 버전 대조는 {@code ./gradlew apiStatusReport} 가 한다.
 */
@SpringBootTest
class PlanDocsReconcileTest {

    @Autowired
    private RequestMappingHandlerMapping mapping;

    @Test
    @DisplayName("기획서에 있는 API 는 전부 라우트가 있다")
    void 기획서의_API_는_모두_구현되어_있다() {
        Set<String> routes = Routes.of(mapping);
        Set<String> missing = new TreeSet<>();

        for (PlanDocs.ApiDoc doc : PlanDocs.loadApis())
            if (!routes.contains(doc.route())) missing.add(doc.id() + "  " + doc.route());

        assertThat(missing)
                .withFailMessage(
                        "기획서에 있는데 서버에 없다 — 아직 안 만들었거나 경로가 바뀌었다:%n  %s%n%n실제 라우트:%n  %s",
                        String.join("\n  ", missing), String.join("\n  ", routes))
                .isEmpty();
    }

    @Test
    @DisplayName("기획서에 없는 라우트를 열어 두지 않는다")
    void 기획서에_없는_API_를_만들지_않는다() {
        Set<String> planned = new TreeSet<>();
        for (PlanDocs.ApiDoc doc : PlanDocs.loadApis()) planned.add(doc.route());

        Set<String> extra = new TreeSet<>(Routes.of(mapping));
        extra.removeAll(planned);

        assertThat(extra)
                .withFailMessage(
                        "기획서 없이 만든 API 다. 문서를 먼저 쓴다 — plan-docs/api/ 에 a-*.md 를 만들고%n"
                                + "version 을 올린 뒤 pnpm api-status:push 를 돌린다:%n  %s",
                        String.join("\n  ", extra))
                .isEmpty();
    }

    @Test
    @DisplayName("기획서의 모든 API 가 version 을 가진다")
    void 버전_없는_문서가_없다() {
        Set<String> noVersion = new TreeSet<>();
        for (PlanDocs.ApiDoc doc : PlanDocs.loadApis())
            if (doc.version() == null || doc.version().isBlank()) noVersion.add(doc.id());

        // version 이 없으면 api-status 에서 어떤 단계도 켜지지 않는다 — 비교할 기준이 없어서다.
        // 조용히 "미완료" 로 보이므로 여기서 잡는다
        assertThat(noVersion)
                .withFailMessage("version 이 없는 기획서 문서: %s", noVersion)
                .isEmpty();
    }
}
