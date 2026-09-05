package com.example.board.planning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * api-status 와 말하는 자리.
 *
 * <p><b>서버가 쓰는 것은 {@code st_server_wip} 와 {@code st_server_done} 뿐이다.</b>
 * 스펙과 {@code version} 은 board-front 가 쓴다 — 임포터가 둘이면 같은 행을 번갈아 덮고,
 * 그때부터 어느 쪽이 원본인지 알 수 없다 (examples/README.md 의 표).
 *
 * <p>그래서 여기에는 {@code POST /import} 도 {@code POST /endpoints} 도 없다.
 * 없는 메서드는 실수로 부를 수도 없다.
 */
public final class ApiStatusClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String base;
    private final String token;
    private final String project;

    private ApiStatusClient(String base, String token, String project) {
        this.base = base;
        this.token = token;
        this.project = project;
    }

    /** 토큰이 없으면 null. <b>실패시키지 않는다</b> — 토큰 없이도 서버는 빌드되고 돌아야 한다 */
    public static ApiStatusClient fromEnvOrNull() {
        String token = System.getenv("API_STATUS_TOKEN");
        if (token == null || token.isBlank()) return null;

        String base = System.getenv().getOrDefault("API_STATUS", "https://api-status.vercel.app");
        String project = System.getenv("API_STATUS_PROJECT");

        // uapi_ 는 여러 프로젝트에 닿으므로 쓰기에서 대상을 밝혀야 한다 (guide.md 0장)
        if (token.startsWith("uapi_") && (project == null || project.isBlank()))
            throw new IllegalStateException(
                    "회원 토큰(uapi_)은 대상 프로젝트가 필요하다. API_STATUS_PROJECT 를 정한다.");

        return new ApiStatusClient(base.replaceAll("/$", ""), token, project);
    }

    /** operation_id → 행. operation_id 가 없는 행은 빠진다 */
    public Map<String, JsonNode> endpointsByOperationId() {
        JsonNode body = send("GET", "/api/v1/endpoints", null);
        Map<String, JsonNode> out = new LinkedHashMap<>();
        for (JsonNode e : body.path("endpoints")) {
            JsonNode op = e.path("operation_id");
            if (!op.isNull() && !op.asText().isBlank()) out.put(op.asText(), e);
        }
        return out;
    }

    /**
     * 단계 하나를 켜거나 끈다.
     *
     * <p><b>보내지 않은 필드는 바뀌지 않는다.</b> 그래서 한 번에 하나만 보낸다 —
     * 셋을 한꺼번에 보내면 남이 켜 둔 것을 실수로 끈다.
     */
    public JsonNode setStage(String endpointId, String field, boolean value) {
        if (!field.equals("st_server_wip") && !field.equals("st_server_done"))
            throw new IllegalArgumentException(
                    "서버가 쓰는 단계는 st_server_wip · st_server_done 뿐이다: " + field);

        return send("PATCH", "/api/v1/endpoints/" + endpointId + "/status",
                "{\"" + field + "\":" + value + "}");
    }

    private JsonNode send(String method, String path, String body) {
        String url = base + path;
        // 읽기는 대상 지정이 선택이지만, 정해져 있으면 늘 붙인다 — 읽기와 쓰기가 다른
        // 범위를 보면 report 가 보여 준 것과 mark 가 바꾸는 것이 어긋난다
        if (project != null && !project.isBlank())
            url += (path.contains("?") ? "&" : "?") + "project=" + URLEncode(project);

        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(30));

        if (body == null) req.method(method, HttpRequest.BodyPublishers.noBody());
        else
            req.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        try {
            HttpResponse<String> res = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 204) return JSON.createObjectNode();
            if (res.statusCode() / 100 != 2) throw new IllegalStateException(explain(method, path, res));
            return JSON.readTree(res.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** 401 은 대개 폐기가 아니라 유효기간이다 (guide.md 0장) */
    private static String explain(String method, String path, HttpResponse<String> res) {
        String hint = switch (res.statusCode()) {
            case 401 -> " — 토큰이 만료됐거나 폐기됐다. 발급 화면에 «만료됨» 으로 뜬다";
            case 404 -> " — 없거나 내 것이 아니다";
            case 400 -> " — 허용되지 않은 필드이거나, 회원 토큰인데 대상 프로젝트를 안 정했다";
            default -> "";
        };
        return method + " " + path + " -> " + res.statusCode() + hint + "\n  " + res.body();
    }

    private static String URLEncode(String v) {
        return java.net.URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
