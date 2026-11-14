---
name: codebase-map
description: 파일 위치를 모를 때 로드. 모듈 구조, 도메인별 주요 클래스 경로, 레거시 src/ 위치, jOOQ DSL 위치.
---

# Codebase Map

> **이 스킬을 쓰는 경우:** "이 클래스 어디 있어요?" 탐색 대신 여기서 찾는다.  
> 새 파일 생성 시 어느 모듈/패키지에 넣을지 판단할 때도 참조.

---

## 모듈 구성

```
dpm-core-server/
├── application/   # 진입점. Controller·Service·Scheduler·EventListener. Spring 의존 OK.
├── domain/        # 순수 도메인. Entity 아님. 프레임워크 의존 금지.
├── persistence/   # JPA Repository + jOOQ 쿼리. PersistencePort 구현체.
├── entity/        # JPA @Entity 클래스만.
├── codegen/       # jOOQ DSL 자동생성 (build 시). 직접 편집 금지.
└── src/           # 레거시 v1 코드. 신규 작업 금지.
```

루트 패키지:
- `application` → `core.application.<도메인>`
- `domain`      → `core.domain.<도메인>`
- `persistence` → `core.persistence.<도메인>`
- `entity`      → `core.entity.<도메인>`

---

## 도메인 목록

`attendance` / `session` / `sessionFeedback` / `cohort` / `member` / `announcement` / `afterParty` / `gathering` / `bill` / `image` / `notification` / `authorization` / `refreshToken`

---

## 레이어별 주요 파일 경로

### application (Controller · Service)

```
core.application.<도메인>/
├── <Domain>CommandService.kt       # 쓰기 서비스
├── <Domain>QueryService.kt         # 읽기 서비스
├── <Domain>CommandController.kt    # 쓰기 Controller 구현체
├── <Domain>QueryController.kt      # 읽기 Controller 구현체
├── <Domain>CommandApi.kt           # Swagger 인터페이스 (쓰기)
├── <Domain>QueryApi.kt             # Swagger 인터페이스 (읽기)
└── <Domain>ExceptionCode.kt        # 에러코드 enum
```

**자주 찾는 클래스:**

| 클래스 | 경로 |
|---|---|
| `AttendanceCommandService` | `application/.../attendance/AttendanceCommandService.kt` |
| `AttendanceQueryService` | `application/.../attendance/AttendanceQueryService.kt` |
| `SessionCommandService` | `application/.../session/SessionCommandService.kt` |
| `SessionFeedbackMyQueryService` | `application/.../sessionFeedback/SessionFeedbackMyQueryService.kt` |
| `SessionFeedbackFormCommandService` | `application/.../sessionFeedback/SessionFeedbackFormCommandService.kt` |
| `CohortQueryService` | `application/.../cohort/CohortQueryService.kt` |
| `AttendancePolicyProperties` | `application/.../attendance/AttendancePolicyProperties.kt` |
| `AttendanceAutoAbsenceScheduler` | `application/.../attendance/AttendanceAutoAbsenceScheduler.kt` |

### domain (Port · 도메인 객체)

```
core.domain.<도메인>/
├── <Domain>.kt                     # 도메인 객체 (비즈니스 로직 포함)
├── <Domain>PersistencePort.kt      # Outbound Port (persistence 구현)
├── <Domain>QueryUseCase.kt         # Inbound Port (application 호출용) — 있는 경우
├── <Domain>CommandUseCase.kt       # Inbound Port — 있는 경우
└── enums/                          # 상태값 enum
```

**자주 찾는 클래스:**

| 클래스 | 경로 |
|---|---|
| `AttendancePersistencePort` | `domain/.../attendance/AttendancePersistencePort.kt` |
| `SessionPersistencePort` | `domain/.../session/SessionPersistencePort.kt` |
| `SessionFeedbackPersistencePort` | `domain/.../sessionFeedback/SessionFeedbackPersistencePort.kt` |
| `SessionFeedbackMyStatus` | `domain/.../sessionFeedback/enums/SessionFeedbackMyStatus.kt` |
| `AttendanceStatus` | `domain/.../attendance/AttendanceStatus.kt` |
| `CohortQueryUseCase` | `domain/.../cohort/CohortQueryUseCase.kt` |

### persistence (Repository · 어댑터)

```
core.persistence.<도메인>/
├── <Domain>Repository.kt           # PersistencePort 구현체 (jOOQ + JPA 혼용)
├── repository/
│   └── <Domain>JpaRepository.kt   # Spring Data JPA interface
└── extension/
    └── QueryConditionExtensions.kt # jOOQ 조건 빌더 확장
```

### entity (JPA Entity)

```
core.entity.<도메인>/
└── <Domain>Entity.kt
```

### jOOQ DSL (자동생성)

```
codegen/build/generated/org/jooq/dsl/
├── tables/          # 테이블별 DSL 클래스 (예: Attendances, Sessions)
└── tables/records/  # 레코드 클래스
```

> `codegen/build/` 아래는 빌드 산출물. 직접 편집하지 않는다.  
> 없으면 `./gradlew :codegen:generateJooq` 실행.

---

## 테스트 파일 위치

```
application/src/test/kotlin/
├── core/application/<도메인>/      # 단위 테스트 (SpringContext 없음)
│   └── <Domain>CommandServiceTest.kt
├── core/application/support/       # 공통 Fixture
│   └── AttendanceTestFixture.kt
└── core/it/<도메인>/               # @Tag("mysql-integration") 통합 테스트
    └── <Domain>MySqlIntegrationTest.kt
```

Contract 테스트: `*ContractTest.kt` — 경로·권한·응답 필드 검증, SpringContext 사용.

---

## 레거시 코드 위치

```
src/main/kotlin/com/server/dpmcore/   # v1 API. 신규 개발 금지.
```

v3 신규 작업은 반드시 `application/` 모듈 아래에 작성.
