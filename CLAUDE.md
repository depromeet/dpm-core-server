# dpm-core-server

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
- 상세: `.claude/skills/hexagonal-arch.md`

## API 작성 규칙

- 응답은 `CustomResponse<T>` 래핑 필수. 데이터 반환 시 `CustomResponse.ok(data)` / `created(data)`, 본문 없음은 `CustomResponse.noContent()`.
- Controller 구현체 / Api 인터페이스(Swagger) 파일 분리. `*Api.kt` + `*Controller.kt`.
- 인증: `@PreAuthorize("hasAuthority('permission:name')")` + `@CurrentMemberId memberId: MemberId`
- 에러코드 형식: `도메인-HTTP상태코드-순번` (예: `ATTENDANCE-403-01`)
- URL 버전 정책:
  - `/v3/` 는 신규 표준. 신규 엔드포인트는 전부 `/v3/`로 시작한다.
  - `/v1/` · `/v2/` 는 application 모듈에서 현재 운영 중(출석·세션 피드백·사유서·공지 등). 레거시 아님.
  - **버전 올리기 규칙**: DTO·파라미터 변경이 생기면 버전을 올리고(`/v2/` → `/v3/`), 기존 버전은 유지한 채 배포. 프론트 작업이 끝나면 기존 버전을 삭제한다.
- 상세: `.claude/skills/api-design.md`

## 커밋 · PR

- 타입: `feat` / `fix` / `refactor` / `docs` / `test` / `chore` / `ci`
- PR 베이스 브랜치: `develop`
- PR 본문 필수 섹션: Summary(Closes #번호) / Tasks / API(변경 시) / ETC
- 상세: `.claude/skills/git-workflow.md`

## 도메인 핵심 상태값

출석: `PENDING` → `PRESENT` / `LATE` / `ABSENT` / `EXCUSED_ABSENT`  
피드백: `AVAILABLE` / `BEFORE_START` / `SUBMITTED` / `EXPIRED` / `ATTENDANCE_INCOMPLETE` / `NOT_TARGET`  
결석 사유서는 제출·재제출하면 `PENDING`으로 저장하고 출석은 그대로 둔다. 운영진이 승인하면 출석을 `EXCUSED_ABSENT`로 바꾸고, 반려하면 출석을 유지한다. 승인된 사유서는 디퍼가 수정·삭제할 수 없다.  
상세: `.claude/skills/domain-model.md`

## 테스트 패턴

- 단위: Fake 포트 + 실제 서비스 조립 (`AttendanceTestFixture` 패턴). SpringContext 없음.
- 통합: `@Tag("mysql-integration")`, 실제 MySQL.
- 계약: `*ContractTest` — 경로·권한·응답 필드 검증.
- 다기수 시나리오 체크리스트 포함. 상세: `.claude/skills/testing-patterns.md`

## 미사용 도메인 (무시)

아래 도메인은 현재 사용하지 않는다. 코드 리뷰·감사·테스트 작성 등 모든 작업에서 고려 대상에서 제외.

- `afterParty` — `application/src/main/kotlin/core/application/afterParty/`
- `bill` — `application/src/main/kotlin/core/application/bill/`
- `gathering` — `application/src/main/kotlin/core/application/gathering/`

## 스킬 목록

| 스킬 | 용도 |
|---|---|
| `.claude/skills/hexagonal-arch.md` | 레이어 경계 규칙 |
| `.claude/skills/domain-model.md` | 도메인 용어·상태값 |
| `.claude/skills/api-design.md` | API 작성 패턴 |
| `.claude/skills/git-workflow.md` | 커밋·PR 규칙 |
| `.claude/skills/testing-patterns.md` | 테스트 작성 규칙 + 다기수 시나리오 |
| `.claude/skills/codebase-map.md` | 모듈·패키지·주요 클래스 위치 |
| `.claude/skills/dev-commands.md` | Gradle 커맨드, 테스트 실행, jOOQ 재생성 |
| `.claude/skills/db-migration.md` | db/pending/ SQL 작성 규칙, 파일명 형식, 배포 순서 |
| `.claude/skills/git-worktree.md` | 병렬 작업 시 worktree 생성·포트 조정·정리 |

## 빌드 환경

- Java 21, Gradle, ktlint (전 서브모듈)
- jOOQ + JPA 혼용 (신규 조회 쿼리는 jOOQ 선호)
- QA 계정 필터: `@dpm.test` 도메인 집계·통계 쿼리에서 제외

## 코드 리뷰

Spring Boot + Kotlin PR 리뷰가 필요하면 `.claude/agents/springboot-kotlin-reviewer.md` 에이전트를 호출한다.  
체크 항목: 헥사고날 위반, Kotlin 관용구, 트랜잭션·동시성, 도메인 상태 전이 오류, 보안.

## 학습 루프

교정 후 재발 방지할 규칙은 `/lesson` 으로 제안. 승인 없이 CLAUDE.md·skills에 직접 쓰지 말 것.  
누적 교정: `.claude/memory/project.md` / 개인·시크릿: `CLAUDE.local.md` (gitignore됨)
