package com.example.board;

/**
 * Railway 에 볼륨 없이 올라가는 것을 막는다.
 *
 * <p>H2 는 파일 모드이고(`application.yml`), 컨테이너의 파일시스템은 재배포와 재시작마다
 * 사라진다. 그런데 {@code ddl-auto: update} 라 스키마는 매번 다시 만들어진다 —
 * <b>에러 없이 빈 게시판이 된다.</b> 터지면 바로 알지만 조용히 비면 한참 뒤에 안다.
 *
 * <p>그래서 뜨기 전에 선다. Railway 는 볼륨을 붙이면 {@code RAILWAY_VOLUME_MOUNT_PATH} 를
 * 스스로 넣어 주므로, 「Railway 위인데 그 값이 없다」가 곧 「볼륨을 안 붙였다」다.
 *
 * <p>일부러 휘발성으로 띄우고 싶으면 {@code BOARD_ALLOW_EPHEMERAL_DB=true} 를 준다.
 * 끄는 길을 열어 두되, 모르고 지나칠 수는 없게 한다.
 */
final class VolumeGuard {

    private VolumeGuard() {}

    static void check(java.util.function.Function<String, String> env) {
        boolean onRailway = env.apply("RAILWAY_ENVIRONMENT") != null
                || env.apply("RAILWAY_PROJECT_ID") != null;
        if (!onRailway) return;

        if (Boolean.parseBoolean(env.apply("BOARD_ALLOW_EPHEMERAL_DB"))) {
            System.out.println(
                    "[VolumeGuard] BOARD_ALLOW_EPHEMERAL_DB=true — 볼륨 없이 뜬다."
                            + " 재배포하면 글이 전부 사라진다");
            return;
        }

        String mount = env.apply("RAILWAY_VOLUME_MOUNT_PATH");
        if (mount == null || mount.isBlank())
            throw new IllegalStateException(String.join(System.lineSeparator(),
                    "",
                    "Railway 인데 볼륨이 없다. H2 는 파일에 쓰고 컨테이너 파일시스템은",
                    "재배포·재시작마다 사라진다 — 그대로 두면 ddl-auto 가 빈 스키마를 다시",
                    "만들어 에러 없이 빈 게시판이 된다.",
                    "",
                    "  Railway 대시보드 → 서비스 → Settings → Volumes → Add Volume",
                    "  마운트 경로는 아무거나 좋다 (예: /data). RAILWAY_VOLUME_MOUNT_PATH 는",
                    "  Railway 가 스스로 넣어 주고, application.yml 이 그 값을 읽는다.",
                    "",
                    "  일부러 휘발성으로 띄우려면 BOARD_ALLOW_EPHEMERAL_DB=true 를 준다.",
                    ""));

        System.out.println("[VolumeGuard] 볼륨 확인: " + mount);
    }
}
