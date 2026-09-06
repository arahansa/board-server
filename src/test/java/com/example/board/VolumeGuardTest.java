package com.example.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 볼륨 없이 Railway 에 올라가는 것을 막는 장치. 실제로 그 일이 일어나면 조용히 빈
 * 게시판이 되므로, 막히는지를 눈으로 믿지 않고 여기서 고정한다.
 *
 * <p>{@code @SpringBootTest} 가 아니다 — 컨텍스트가 뜨기 전에 도는 검사라서다.
 */
class VolumeGuardTest {

    /** {@code System::getenv} 자리에 끼우는 가짜 환경 */
    private static java.util.function.Function<String, String> env(String... kv) {
        Map<String, String> m = new java.util.HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m::get;
    }

    @Test
    @DisplayName("Railway 가 아니면 아무것도 하지 않는다 — 로컬 개발이 막히면 안 된다")
    void 로컬은_통과한다() {
        assertThatCode(() -> VolumeGuard.check(env())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("★ Railway 인데 볼륨이 없으면 뜨지 않는다")
    void 볼륨_없는_Railway_는_선다() {
        assertThatThrownBy(() -> VolumeGuard.check(env("RAILWAY_ENVIRONMENT", "production")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Add Volume")
                .hasMessageContaining("BOARD_ALLOW_EPHEMERAL_DB");
    }

    @Test
    @DisplayName("빈 문자열도 없는 것으로 본다 — 변수만 만들어 두고 볼륨은 안 붙인 경우다")
    void 빈_마운트경로는_없는_것이다() {
        assertThatThrownBy(() -> VolumeGuard.check(
                        env("RAILWAY_PROJECT_ID", "p-1", "RAILWAY_VOLUME_MOUNT_PATH", "  ")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("볼륨이 있으면 통과한다")
    void 볼륨이_있으면_통과한다() {
        assertThatCode(() -> VolumeGuard.check(
                        env("RAILWAY_ENVIRONMENT", "production", "RAILWAY_VOLUME_MOUNT_PATH", "/data")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("일부러 휘발성으로 띄우는 길은 열어 둔다")
    void 명시적으로_끄면_통과한다() {
        assertThatCode(() -> VolumeGuard.check(
                        env("RAILWAY_ENVIRONMENT", "production", "BOARD_ALLOW_EPHEMERAL_DB", "true")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("탈출구는 true 일 때만 열린다 — 오타로 열리지 않는다")
    void 아무_값이나_통과시키지_않는다() {
        assertThat(Boolean.parseBoolean("yes")).isFalse();
        assertThatThrownBy(() -> VolumeGuard.check(
                        env("RAILWAY_ENVIRONMENT", "production", "BOARD_ALLOW_EPHEMERAL_DB", "yes")))
                .isInstanceOf(IllegalStateException.class);
    }
}
