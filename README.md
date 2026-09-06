---
version: "1.1"
created: "2026-09-05"
updated: "2026-09-06"
author: "arahansa"
---

# board-server

게시판 예제의 서버. Spring Boot 3.5 · Java 17 · H2 · JPA · JWT.

[api-status](https://github.com/arahansa/api-status) 의 `examples/board-server` 를
떼어낸 저장소다. 짝이 되는 프론트(`board-front`)와 두 프로젝트의 관계 설명은
그쪽 `examples/README.md` 에 있다.

**이 서버는 기획서를 진실로 삼는다.** 새 API 를 여기서 먼저 만들지 않는다 —
문서를 먼저 쓰고, 그러면 `PlanDocsReconcileTest` 가 빨개진다.

## 띄우기

```bash
./gradlew bootRun     # http://localhost:8080/api
```

H2 는 파일 모드다(`./data/board`). 껐다 켜도 글이 남아 있어야 워크플로우를
이어서 시연할 수 있다. H2 콘솔은 `/api/h2` 다.

경로에 `/api` 가 붙는 것은 `context-path` 다. **기획서의 경로에는 `/api` 가 없고**,
그래서 Spring 의 핸들러 매핑 패턴이 문서와 글자 그대로 같아진다 —
대조 테스트가 접두사를 벗기는 보정을 하지 않아도 된다.

## ★ 대조 테스트

```bash
./gradlew test        # 22개 (기획서가 없으면 19개 + skip 3개)
```

`PlanDocsReconcileTest` 가 `board-front/plan-docs/api/**` 를 읽어
실제 라우트(`RequestMappingHandlerMapping`)와 맞춰 본다.

| 어긋남 | 뜻 | 결과 |
|---|---|---|
| 문서에 있는데 라우트가 없다 | 아직 안 만든 API | **실패** |
| 라우트가 있는데 문서에 없다 | 기획서 없이 만든 API | **실패** |
| 문서에 `version` 이 없다 | api-status 에서 어떤 단계도 안 켜진다 | **실패** |

**왜 테스트인가.** 문서와 코드가 갈라지는 것은 조용히 일어난다. 사람이 눈으로
대조하면 몇 번 하다가 안 하게 된다. 빌드가 서면 안 할 수가 없다.

기획서 위치는 `PLAN_DOCS_DIR` 이 정하고, 기본값은 `../board-front/plan-docs` 다
(`build.gradle`). 두 프로젝트가 나란히 있다는 전제가 거기 한 줄에 모여 있다.

**이 저장소에는 기획서가 없다.** 스펙의 주인이 하나여야 해서 복사본을 두지 않았다.
그래서 이 저장소만 클론하면 `PlanDocsReconcileTest` 3개는 **실패가 아니라 skip** 이고(19 passed · 3 skipped),
어디를 봤는지를 출력한다. 없는 문서를 「어긋남」으로 볼 수는 없기 때문이다.
기획서를 옆에 두거나 경로를 주면 그때부터 다시 문다.

```bash
git clone https://github.com/arahansa/api-status.git
PLAN_DOCS_DIR=$PWD/api-status/examples/board-front/plan-docs ./gradlew test
```

## api-status

```bash
export API_STATUS_TOKEN=mapi_...        # /projects/<slug>/settings 에서 발급
export API_STATUS=https://api-status.vercel.app

./gradlew apiStatusReport               # 기획서 · 버전 · 단계를 나란히
./gradlew apiStatusMarkWip  -Papi=A-100 # 서버 착수
./gradlew apiStatusMarkDone -Papi=A-100 # 서버 완료 (-Papi 없으면 전부)
```

**토큰이 없으면 건너뛰고 그렇게 말한다.** 실패시키지 않는다 —
토큰 없이도 서버는 빌드되고 돌아야 한다.

`apiStatusReport` 가 보여 주는 것 중 눈여겨볼 것은 마지막 열이다.

```
A-100   GET      /posts                 260905-2   260905-2   착수 서버완료 ← 260905-1 기준으로 끝났다
```

「260905-1 기준으로 끝냈다」는 **스펙이 바뀌었으니 다시 보라**는 뜻이다.
기획서에서 `version` 을 올리고 `pnpm api-status:push` 를 돌리면 저절로 이렇게 된다.

### 서버가 쓰는 것은 두 필드뿐이다

| 필드 | 누가 |
|---|---|
| `st_server_wip` · `st_server_done` | **여기** |
| `spec` · `summary` · `tag` · `version` | board-front |
| `st_connected` | board-front |

`ApiStatusClient` 에는 `POST /import` 도 `POST /endpoints` 도 **없다.**
없는 메서드는 실수로 부를 수도 없다 — 임포터가 둘이면 같은 행을 번갈아 덮고,
그때부터 어느 쪽이 원본인지 알 수 없다.

## 인증

JWT(HS256) 를 `Authorization: Bearer` 로 싣는다. 쿠키가 아니라 CSRF 가 성립하지 않고,
CORS 설정도 없다 — vite 가 `/api` 를 프록시해 같은 출처가 된다.

**로그아웃이 실제로 끊는다.** JWT 는 상태가 없어 폐기가 원래 없는데,
회원에 `tokenVersion` 을 두고 클레임에 실었다. 로그아웃과 비밀번호 변경이 그 값을
올리면 이전 토큰이 전부 거절된다. 기기 전체가 함께 로그아웃되는 것은 이 방식의
성질이고, 숨기지 않는 편이 정직하다.

`board.jwt.secret` 은 예제 기본값이 박혀 있다. 실제로는 `BOARD_JWT_SECRET` 으로 준다 —
이 값이 새면 토큰을 위조할 수 있다.

## 구조

```
domain/     User · Post · 리포지토리
security/   JwtService · JwtAuthFilter · SecurityConfig
web/        컨트롤러 3개 · DTO · 예외 처리
planning/   ★ PlanDocs(기획서 파서) · Routes · ApiStatusClient · ApiStatusTool
```

`planning/` 이 이 예제가 존재하는 이유다. 나머지는 그것이 볼 대상이다.
