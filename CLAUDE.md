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

- 응답은 `CustomResponse<T>` 래핑 필수. `CustomResponse.ok(data)` 사용.
- Controller 구현체 / Api 인터페이스(Swagger) 파일 분리. `*Api.kt` + `*Controller.kt`.
- 인증: `@PreAuthorize("hasAuthority('permission:name')")` + `@CurrentMemberId Long memberId`
- 에러코드 형식: `도메인-HTTP상태코드-순번` (예: `ATTENDANCE-403-01`)
- URL: `/v3/` 신규 기준. `/v1/` 은 레거시(src/ 모듈) 호환용.
- 상세: `.claude/skills/api-design.md`

## 커밋 · PR

- 타입: `feat` / `fix` / `refactor` / `docs` / `test` / `chore` / `ci`
- PR 베이스 브랜치: `develop`
- PR 본문 필수 섹션: Summary(Closes #번호) / Tasks / API(변경 시) / ETC
- 상세: `.claude/skills/git-workflow.md`

## 도메인 핵심 상태값

출석: `PENDING` → `PRESENT` / `LATE` / `ABSENT` / `EXCUSED_ABSENT`  
피드백: `AVAILABLE` / `BEFORE_START` / `SUBMITTED` / `EXPIRED` / `ATTENDANCE_INCOMPLETE` / `NOT_TARGET`  
결석사유서 제출 → 즉시 `EXCUSED_ABSENT` (반려해도 출석 상태는 유지됨)  
상세: `.claude/skills/domain-model.md`

## 테스트 패턴

- 단위: Fake 포트 + 실제 서비스 조립 (`AttendanceTestFixture` 패턴). SpringContext 없음.
- 통합: `@Tag("mysql-integration")`, 실제 MySQL.
- 계약: `*ContractTest` — 경로·권한·응답 필드 검증.
- 다기수 시나리오 체크리스트 포함. 상세: `.claude/skills/testing-patterns.md`

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
