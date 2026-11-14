# dpm-core-server

## 커밋·PR 작업자

- GitHub PR **작업자(co-authors)** / 커밋 작성자에 Cursor를 넣지 않는다.
- `Co-authored-by: Cursor <cursoragent@cursor.com>` trailer를 커밋 메시지에 포함하지 않는다.
- `Made with Cursor` 문구를 커밋·PR 본문에 넣지 않는다.
- 커밋 후 trailer가 붙었다면 제거하고, 이미 push됐으면 `--force-with-lease`로 맞춘다 (이 저장소 작업 브랜치에 한함, `main`/`master` force 금지).

---

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
- 상세: `.agents/skills/hexagonal-arch/SKILL.md`

## API 작성 규칙

- 응답은 `CustomResponse<T>` 래핑 필수. 데이터 반환 시 `CustomResponse.ok(data)` / `created(data)`, 본문 없음은 `CustomResponse.noContent()`.
- Controller 구현체 / Api 인터페이스(Swagger) 파일 분리. `*Api.kt` + `*Controller.kt`.
- 인증: `@PreAuthorize("hasAuthority('permission:name')")` + `@CurrentMemberId memberId: MemberId`
- 에러코드 형식: `도메인-HTTP상태코드-순번` (예: `ATTENDANCE-403-01`)
- URL 버전 정책:
  - `/v3/` 는 신규 표준. 신규 엔드포인트는 전부 `/v3/`로 시작한다.
  - `/v1/` · `/v2/` 는 application 모듈에서 현재 운영 중(출석·세션 피드백·사유서·공지 등). 레거시 아님.
  - **버전 올리기 규칙**: DTO·파라미터 변경이 생기면 버전을 올리고(`/v2/` → `/v3/`), 기존 버전은 유지한 채 배포. 프론트 작업이 끝나면 기존 버전을 삭제한다.
- 상세: `.agents/skills/api-design/SKILL.md`

## 커밋 · PR

- 타입: `feat` / `fix` / `refactor` / `docs` / `test` / `chore` / `ci`
- PR 베이스 브랜치: `develop`
- PR 본문 필수 섹션: Summary(Closes #번호) / Tasks / API(변경 시) / ETC
- Assignee: `@me`, Reviewer: `hwistlezz`, `cowboysj`
- 상세: `.agents/skills/git-workflow/SKILL.md`

## 도메인 핵심 상태값

출석: `PENDING` → `PRESENT` / `LATE` / `ABSENT` / `EXCUSED_ABSENT`
피드백: `AVAILABLE` / `BEFORE_START` / `SUBMITTED` / `EXPIRED` / `ATTENDANCE_INCOMPLETE` / `NOT_TARGET`
결석 사유서는 제출·재제출하면 `PENDING`으로 저장하고 출석은 그대로 둔다. 운영진이 승인하면 출석을 `EXCUSED_ABSENT`로 바꾸고, 반려하면 출석을 유지한다. 승인된 사유서는 디퍼가 수정·삭제할 수 없다.
상세: `.agents/skills/domain-model/SKILL.md`

## 미사용 도메인 (무시)

아래 도메인은 현재 사용하지 않는다. 코드 리뷰·감사·테스트 작성 등 모든 작업에서 고려 대상에서 제외.

- `afterParty` — `application/src/main/kotlin/core/application/afterParty/`
- `bill` — `application/src/main/kotlin/core/application/bill/`
- `gathering` — `application/src/main/kotlin/core/application/gathering/`

## 테스트 패턴

- 단위: Fake 포트 + 실제 서비스 조립 (`AttendanceTestFixture` 패턴). SpringContext 없음.
- 통합: `@Tag("mysql-integration")`, 실제 MySQL.
- 계약: `*ContractTest` — 경로·권한·응답 필드 검증.
- 다기수 시나리오 체크리스트 포함. 상세: `.agents/skills/testing-patterns/SKILL.md`

## 빌드 환경

- Java 21, Gradle, ktlint (전 서브모듈)
- jOOQ + JPA 혼용 (신규 조회 쿼리는 jOOQ 선호)
- QA 계정 필터: `@dpm.test` 도메인 집계·통계 쿼리에서 제외

## 스킬 목록

스킬 파일 위치: **Codex** → `.agents/skills/<이름>/SKILL.md` / **Claude Code** → `.claude/skills/<이름>.md`

| 스킬 | 용도 |
|---|---|
| `hexagonal-arch` | 레이어 경계 규칙 |
| `domain-model` | 도메인 용어·상태값 |
| `api-design` | API 작성 패턴 |
| `git-workflow` | 커밋·PR 규칙 |
| `testing-patterns` | 테스트 작성 규칙 + 다기수 시나리오 |
| `multi-cohort-scenarios` | 다기수 페르소나 5종 |
| `codebase-map` | 패키지·클래스 위치 맵 |
| `dev-commands` | Gradle 빌드·테스트 커맨드 |
| `db-migration` | DB 마이그레이션 규칙 |
| `git-worktree` | Worktree + 포트 충돌 방지 |

## 에이전트 목록

| 에이전트 | 역할 | Claude Code | Codex |
|---|---|---|---|
| `feature-planner` | 요구사항 분석·계획 수립 | `.claude/agents/feature-planner.md` | `.codex/agents/feature-planner.toml` |
| `feature-implementer` | 계획 받아 구현 (병렬 하위 에이전트 조율) | `.claude/agents/feature-implementer.md` | `.codex/agents/feature-implementer.toml` |
| `bugfix-investigator` | 버그 원인 특정·수정 계획 | `.claude/agents/bugfix-investigator.md` | `.codex/agents/bugfix-investigator.toml` |
| `springboot-kotlin-reviewer` | 헥사고날·트랜잭션·보안 리뷰 | `.claude/agents/springboot-kotlin-reviewer.md` | `.codex/agents/springboot-kotlin-reviewer.toml` |
| `code-writer` | 단일 Kotlin 파일 작성 (병렬 호출용) | `.claude/agents/code-writer.md` | `.codex/agents/code-writer.toml` |
| `test-writer` | 단위·계약·통합 테스트 작성 (병렬 호출용) | `.claude/agents/test-writer.md` | `.codex/agents/test-writer.toml` |
| `doc-writer` | PR 본문·SQL 헤더·커밋 메시지 작성 (병렬 호출용) | `.claude/agents/doc-writer.md` | `.codex/agents/doc-writer.toml` |

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

## 세션 메모리

매 세션 시작 시 `.agents/memory/project.md` 를 확인해 누적 교정 사항을 파악한다.
교정 사항 추가는 아래 워크플로우 에이전트를 사용한다.

## 행동 규칙 (Codex 훅 등가)

Codex에는 훅이 없으므로 아래 규칙을 항상 직접 실행한다.

| 시점 | 규칙 |
|---|---|
| `AGENTS.md` / `.agents/skills/` 편집 전 | lesson 흐름을 거쳤는지 확인. 아니면 편집 중단 후 lesson 에이전트 실행 |
| `git push` / `gh pr create` 전 | `./gradlew ktlintCheck` 실행. 위반 시 `./gradlew ktlintFormat` 후 재시도 |
| `gh pr create` 전 | `.agents/skills/multi-cohort-scenarios/SKILL.md` 체크리스트 확인 |
| 세션 종료 시 | 교정 사항이 있었다면 lesson 에이전트를 실행해 규칙화 고려 |

## 워크플로우

**Claude Code**: `/lesson` · `/feature` · `/bugfix` 커맨드 사용  
**Codex**: `.codex/agents/lesson.toml` · `.codex/agents/feature.toml` · `.codex/agents/bugfix.toml` 에이전트 사용

| 워크플로우 | 역할 | Claude Code | Codex |
|---|---|---|---|
| `lesson` | 교정 → 규칙화 → 메모리 저장 | `/lesson` | `.codex/agents/lesson.toml` |
| `feature` | 플래닝→구현→리뷰→PR 오케스트레이터 | `/feature` | `.codex/agents/feature.toml` |
| `bugfix` | 원인 파악→수정→리뷰→PR 오케스트레이터 | `/bugfix` | `.codex/agents/bugfix.toml` |
