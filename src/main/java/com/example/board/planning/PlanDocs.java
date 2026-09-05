package com.example.board.planning;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * board-front 의 {@code plan-docs/api/**} 를 읽는다.
 *
 * <p><b>기획서가 스펙의 SSOT 다.</b> 서버는 그것을 읽을 뿐 고치지 않는다 — 서버가 문서를
 * 고치기 시작하면 기획과 구현 중 어느 쪽이 원본인지 흐려진다.
 *
 * <p>마크다운 라이브러리를 들이지 않았다. 읽는 것은 frontmatter 의 몇 줄과
 * {@code ## 명세} 표 하나뿐이고, 규격은 {@code plan-docs/README.md} 가 정한다.
 * 파서가 크면 규격보다 파서가 관대해져서 문서가 조용히 규격을 벗어난다.
 */
public final class PlanDocs {

    /** 기획서 API 문서 하나. 대조에 필요한 것만 든다 */
    public record ApiDoc(String id, String name, String method, String path, String version, Path file) {
        public String route() {
            return method + " " + path;
        }
    }

    private static final Pattern FRONT = Pattern.compile("^---\\R(.*?)\\R---\\R", Pattern.DOTALL);
    private static final Pattern SPEC_ROW = Pattern.compile("^\\|([^|]*)\\|([^|]*)\\|\\s*$");

    private PlanDocs() {}

    /** {@code -Dplan.docs.dir} 또는 {@code PLAN_DOCS_DIR}. build.gradle 이 기본값을 준다 */
    public static Path dir() {
        String at = System.getProperty("plan.docs.dir", System.getenv("PLAN_DOCS_DIR"));
        if (at == null)
            throw new IllegalStateException(
                    "plan.docs.dir 이 없다. board-front 의 plan-docs 경로를 준다 (build.gradle 참고)");
        return Path.of(at);
    }

    public static List<ApiDoc> loadApis() {
        return loadApis(dir());
    }

    public static List<ApiDoc> loadApis(Path planDocs) {
        Path apiDir = planDocs.resolve("api");
        if (!Files.isDirectory(apiDir))
            throw new IllegalStateException("기획서를 찾지 못했다: " + apiDir.toAbsolutePath());

        try (Stream<Path> files = Files.walk(apiDir)) {
            List<ApiDoc> out = new ArrayList<>();
            for (Path f : files.filter(p -> p.getFileName().toString().startsWith("a-"))
                    .filter(p -> p.toString().endsWith(".md"))
                    .sorted()
                    .toList()) {
                ApiDoc doc = parse(f);
                if (doc != null) out.add(doc);
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ApiDoc parse(Path file) {
        String text;
        try {
            text = Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Map<String, String> front = frontmatter(text);
        String id = front.get("id");
        if (id == null) return null;

        Map<String, String> spec = specTable(text);
        String method = spec.get("메서드");
        String path = spec.get("경로");
        if (method == null || path == null)
            throw new IllegalStateException(file + ": '## 명세' 표에 메서드·경로가 없다");

        return new ApiDoc(
                id,
                front.getOrDefault("name", id),
                method.toUpperCase(),
                path,
                front.get("version"),
                file);
    }

    /** `키: 값` 한 줄짜리만 읽는다. 목록(`- 항목`)은 대조에 쓰이지 않는다 */
    private static Map<String, String> frontmatter(String text) {
        Matcher m = FRONT.matcher(text);
        if (!m.find()) return Map.of();

        Map<String, String> out = new java.util.LinkedHashMap<>();
        for (String line : m.group(1).split("\\R")) {
            int at = line.indexOf(':');
            if (at <= 0 || line.startsWith(" ") || line.startsWith("-")) continue;
            out.put(line.substring(0, at).trim(), unquote(line.substring(at + 1).trim()));
        }
        return out;
    }

    /** `## 명세` 절의 두 칸짜리 표 — 행 이름이 곧 규격이다 (plan-docs/README.md) */
    private static Map<String, String> specTable(String text) {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        boolean inSection = false;

        for (String line : text.split("\\R")) {
            if (line.startsWith("## ")) {
                if (inSection) break;
                inSection = line.trim().equals("## 명세");
                continue;
            }
            if (!inSection) continue;

            Matcher m = SPEC_ROW.matcher(line);
            if (!m.matches()) continue;
            String key = strip(m.group(1));
            String value = strip(m.group(2));
            if (!key.isEmpty()) out.put(key, value);
        }
        return out;
    }

    private static String strip(String v) {
        return v.replace("`", "").trim();
    }

    private static String unquote(String v) {
        return v.replaceAll("^[\"'](.*)[\"']$", "$1");
    }
}
