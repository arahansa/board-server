package com.example.board.planning;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/**
 * {@code ./gradlew apiStatusReport · apiStatusMarkWip · apiStatusMarkDone} 의 알맹이.
 *
 * <p>Spring 을 띄우지 않는다. 라우트가 필요 없고 — 그건 대조 테스트가 이미 본다 —
 * 컨텍스트를 띄우면 이 도구를 쓰려고 DB 가 있어야 한다.
 */
public final class ApiStatusTool {

    public static void main(String[] args) {
        String action = args.length > 0 ? args[0] : "report";
        String only = args.length > 1 ? args[1] : null;

        List<PlanDocs.ApiDoc> docs = PlanDocs.loadApis();
        ApiStatusClient client = ApiStatusClient.fromEnvOrNull();

        if (client == null) {
            // 실패시키지 않는다. 토큰 없이도 서버는 빌드되고 돌아야 한다
            System.out.println("API_STATUS_TOKEN 이 없어 api-status 를 건너뛴다.");
            System.out.println("기획서 " + docs.size() + "개는 읽었다 (" + PlanDocs.dir() + ").");
            return;
        }

        Map<String, JsonNode> rows = client.endpointsByOperationId();
        switch (action) {
            case "report" -> report(docs, rows);
            case "wip" -> mark(client, docs, rows, "st_server_wip", only);
            case "done" -> mark(client, docs, rows, "st_server_done", only);
            default -> throw new IllegalArgumentException("모르는 명령: " + action);
        }
    }

    private static void report(List<PlanDocs.ApiDoc> docs, Map<String, JsonNode> rows) {
        System.out.printf("%-7s %-8s %-22s %-10s %-10s %s%n",
                "ID", "메서드", "경로", "문서버전", "상태버전", "단계");
        System.out.println("-".repeat(86));

        int drifted = 0;
        int missing = 0;

        for (PlanDocs.ApiDoc doc : docs) {
            JsonNode row = rows.get(doc.id());
            if (row == null) {
                missing++;
                System.out.printf("%-7s %-8s %-22s %-10s %-10s %s%n",
                        doc.id(), doc.method(), doc.path(), doc.version(), "-", "api-status 에 없음");
                continue;
            }

            String remote = row.path("version").asText(null);
            String stages = (row.path("st_server_wip").asBoolean() ? "착수 " : "")
                    + (row.path("st_server_done").asBoolean() ? "서버완료 " : "")
                    + (row.path("st_connected").asBoolean() ? "연결 " : "");
            if (row.path("is_stale").asBoolean()) stages += "[stale] ";

            // 끝냈다고 표시했는데 그때의 버전이 지금 버전과 다르다 — 다시 봐야 한다는 뜻
            String doneAt = row.path("st_server_done_version").asText(null);
            boolean drift = doneAt != null && !doneAt.equals(remote);
            if (drift) {
                drifted++;
                stages += "← " + doneAt + " 기준으로 끝냈다";
            }

            System.out.printf("%-7s %-8s %-22s %-10s %-10s %s%n",
                    doc.id(), doc.method(), doc.path(), doc.version(),
                    remote == null ? "-" : remote, stages.isBlank() ? "-" : stages.trim());
        }

        System.out.println();
        if (missing > 0)
            System.out.println(missing + "개가 api-status 에 없다 — board-front 에서 "
                    + "pnpm api-status:push 를 돌린다.");
        if (drifted > 0)
            System.out.println(drifted + "개가 옛 버전 기준으로 완료 표시돼 있다. "
                    + "스펙이 바뀌었으니 다시 본다.");
        if (missing == 0 && drifted == 0) System.out.println("어긋난 것 없음.");
    }

    private static void mark(
            ApiStatusClient client,
            List<PlanDocs.ApiDoc> docs,
            Map<String, JsonNode> rows,
            String field,
            String only) {

        List<PlanDocs.ApiDoc> targets =
                only == null ? docs : docs.stream().filter(d -> d.id().equals(only)).toList();

        if (targets.isEmpty())
            throw new IllegalArgumentException("기획서에 없는 아이디다: " + only);

        for (PlanDocs.ApiDoc doc : targets) {
            JsonNode row = rows.get(doc.id());
            if (row == null) {
                System.out.println("  " + doc.id() + " — api-status 에 없다. 건너뛴다");
                continue;
            }
            // version 이 null 인 행에 true 를 보내면 저장할 값이 없어 꺼진 채로 남는다 (guide.md 6장)
            if (row.path("version").isNull()) {
                System.out.println("  " + doc.id() + " — version 이 없어 켜지지 않는다. "
                        + "먼저 pnpm api-status:push 를 돌린다");
                continue;
            }

            JsonNode after = client.setStage(row.path("id").asText(), field, true);
            System.out.println("  " + doc.id() + "  " + field + " = true  (버전 "
                    + after.path("version").asText() + ")");
        }
    }

    private ApiStatusTool() {}
}
