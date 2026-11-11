# Agent 지침

## 커밋·PR 작업자

- GitHub PR **작업자(co-authors)** / 커밋 작성자에 Cursor를 넣지 않는다.
- `Co-authored-by: Cursor <cursoragent@cursor.com>` trailer를 커밋 메시지에 포함하지 않는다.
- `Made with Cursor` 문구를 커밋·PR 본문에 넣지 않는다.
- 커밋 후 trailer가 붙었다면 제거하고, 이미 push됐으면 `--force-with-lease`로 맞춘다 (이 저장소 작업 브랜치에 한함, `main`/`master` force 금지).

---

# dpm-core-server (Codex 용)

> CLAUDE.md 와 동일한 내용. 경로만 `.codex/` 기준으로 표기.

## 모듈 구조

```
domain → application → persistence / entity
codegen → (jOOQ 클래스 생성, build 의존만)
```

의존 방향은 단방향. `application`이 `persistence` 패키지를 직접 import하면 위반.

## 헥사고날 규칙

- **domain**: 프레임워크 의존 금지. `@Service` / `@Repository` / `@Transactional` 사용 불가.
- **application**: JPA Entity 직접 참조 금지. persistence에 접근할 땐 Port 인터페이스만.
- **Port 네이밍**: outbound → `*PersistencePort` / inbound → `*QueryUseCase`, `*CommandUseCase`
- 상세: `.codex/skills/hexagonal-arch.md`

## API 작성 규칙

- 응답은 `CustomResponse<T>` 래핑 필수. `CustomResponse.ok(data)` 사용.
- Controller 구현체 / Api 인터페이스(Swagger) 파일 분리. `*Api.kt` + `*Controller.kt`.
- 인증: `@PreAuthorize("hasAuthority('permission:name')")` + `@CurrentMemberId Long memberId`
- 에러코드 형식: `도메인-HTTP상태코드-순번` (예: `ATTENDANCE-403-01`)
- URL: `/v3/` 신규 기준. `/v1/` 은 레거시(src/ 모듈) 호환용.
- 상세: `.codex/skills/api-design.md`

## 커밋 · PR

- 타입: `feat` / `fix` / `refactor` / `docs` / `test` / `chore` / `ci`
- PR 베이스 브랜치: `develop`
- PR 본문 필수 섹션: Summary(Closes #번호) / Tasks / API(변경 시) / ETC
- Assignee: `@me`, Reviewer: `hwistlezz`, `cowboysj`
- 상세: `.codex/skills/git-workflow.md`

## 도메인 핵심 상태값

출석: `PENDING` → `PRESENT` / `LATE` / `ABSENT` / `EXCUSED_ABSENT`
피드백: `AVAILABLE` / `BEFORE_START` / `SUBMITTED` / `EXPIRED` / `ATTENDANCE_INCOMPLETE` / `NOT_TARGET`
결석사유서 제출 → 즉시 `EXCUSED_ABSENT` (반려해도 출석 상태는 유지됨)
상세: `.codex/skills/domain-model.md`

## 테스트 패턴

- 단위: Fake 포트 + 실제 서비스 조립. SpringContext 없음.
- 통합: `@Tag("mysql-integration")`, 실제 MySQL.
- 계약: `*ContractTest` — 경로·권한·응답 필드 검증.
- 다기수 시나리오 체크리스트 포함. 상세: `.codex/skills/testing-patterns.md`

## 스킬 목록

| 스킬 | 용도 |
|---|---|
| `.codex/skills/hexagonal-arch.md` | 레이어 경계 규칙 |
| `.codex/skills/domain-model.md` | 도메인 용어·상태값 |
| `.codex/skills/api-design.md` | API 작성 패턴 |
| `.codex/skills/git-workflow.md` | 커밋·PR 규칙 |
| `.codex/skills/testing-patterns.md` | 테스트 작성 규칙 + 다기수 시나리오 |
| `.codex/skills/multi-cohort-scenarios.md` | 다기수 페르소나 5종 |
| `.codex/skills/codebase-map.md` | 패키지·클래스 위치 맵 |
| `.codex/skills/dev-commands.md` | Gradle 빌드·테스트 커맨드 |
| `.codex/skills/db-migration.md` | DB 마이그레이션 규칙 |
| `.codex/skills/git-worktree.md` | Worktree + 포트 충돌 방지 |

## 에이전트 목록

| 에이전트 | 역할 |
|---|---|
| `.codex/agents/feature-planner.md` | 요구사항 분석·계획 수립 |
| `.codex/agents/feature-implementer.md` | 계획 받아 구현 (병렬 하위 에이전트 조율) |
| `.codex/agents/bugfix-investigator.md` | 버그 원인 특정·수정 계획 |
| `.codex/agents/springboot-kotlin-reviewer.md` | 헥사고날·트랜잭션·보안 리뷰 |
| `.codex/agents/code-writer.md` | 단일 Kotlin 파일 작성 (병렬 호출용) |
| `.codex/agents/test-writer.md` | 단위·계약·통합 테스트 작성 (병렬 호출용) |
| `.codex/agents/doc-writer.md` | PR 본문·SQL 헤더·커밋 메시지 작성 (병렬 호출용) |

### 병렬 에이전트 패턴

기능 구현 시 application 레이어 작성을 병렬로 진행한다:

```
# code-writer: 도메인별 동시 호출
Agent(code-writer): XxxCommandService 작성
Agent(code-writer): XxxQueryService 작성       ← 동시에
Agent(code-writer): XxxCommandController 작성  ← 동시에

# test-writer + doc-writer 동시 호출
Agent(test-writer): XxxCommandServiceTest 작성
Agent(doc-writer):  PR 본문 초안 작성           ← 동시에
```

## 워크플로우

- `/feature` → feature-planner → feature-implementer → springboot-kotlin-reviewer → PR
- `/bugfix` → bugfix-investigator → 최소 수정 → springboot-kotlin-reviewer → PR

## 빌드 환경

- Java 21, Gradle, ktlint (전 서브모듈)
- jOOQ + JPA 혼용 (신규 조회 쿼리는 jOOQ 선호)
- QA 계정 필터: `@dpm.test` 도메인 집계·통계 쿼리에서 제외
