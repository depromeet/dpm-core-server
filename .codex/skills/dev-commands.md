---
name: dev-commands
description: 빌드·테스트·포맷 Gradle 커맨드, 테스트 태그 사용법, jOOQ 재생성 방법.
---

# Dev Commands

> **이 스킬을 쓰는 경우:** Gradle 커맨드가 헷갈릴 때, 테스트를 선택적으로 실행하고 싶을 때.

---

## 테스트

### 단위 테스트만 (기본)

```bash
./gradlew test
```

- `@Tag("mysql-integration")` 붙은 테스트는 **자동 제외**됨
- SpringContext 없는 Fake 포트 기반 테스트 포함
- CI 기본 실행 대상

### 통합 테스트만 (MySQL 필요)

```bash
# 환경변수 필수. 미설정 시 @EnabledIfEnvironmentVariable 로 silently skip된다.
# 실제 연결 정보는 CLAUDE.local.md 참고
DPM_IT_MYSQL_URL='...' \
DPM_IT_MYSQL_USERNAME='...' \
DPM_IT_MYSQL_PASSWORD='...' \
./gradlew :application:mysqlIntegrationTest
```

- `@Tag("mysql-integration")` 붙은 테스트만 실행
- 로컬 MySQL이 떠 있어야 함
- 실행 순서: `test` → `mysqlIntegrationTest` (shouldRunAfter 설정)
- **현재 CI는 실행하지 않음** → PR 올리기 전 수동 실행 결과를 PR 본문에 기록

### 전체 테스트

```bash
./gradlew test mysqlIntegrationTest
```

### 특정 클래스/도메인만

```bash
./gradlew test --tests "core.application.attendance.*"
./gradlew test --tests "core.application.attendance.AttendanceCommandServiceTest"
```

---

## 빌드

### 전체 빌드

```bash
./gradlew build
```

### 테스트 제외 빌드 (빠른 컴파일 확인)

```bash
./gradlew build -x test
```

### 특정 모듈만

```bash
./gradlew :application:build
./gradlew :domain:build
```

---

## 코드 포맷

### ktlint 자동 포맷 (전체)

```bash
./gradlew ktlintFormat
```

### ktlint 검사만 (포맷 미적용)

```bash
./gradlew ktlintCheck
```

> **PR 올리기 전**에 `ktlintCheck`로 포맷 검증을 한 번 돌린다. (설정된 PreToolUse 훅이 `git push`/`gh pr create` 직전에 자동으로 실행한다.)
> `ktlintFormat`은 전체 모듈을 돌아서 느리므로, 편집 중에는 필요할 때만 수동 실행.

---

## jOOQ DSL 재생성

```bash
./gradlew :codegen:generateJooq
```

- DB 스키마 변경(마이그레이션) 후 실행
- 생성 위치: `codegen/build/generated/org/jooq/dsl/`
- DB 연결 정보 필요 (로컬 MySQL)

---

## 테스트 태그 작성법

```kotlin
@Tag("mysql-integration")
class AttendanceOverviewMySqlIntegrationTest { ... }
```

| 태그 | 실행 커맨드 | 용도 |
|---|---|---|
| (없음) | `./gradlew test` | 단위·계약 테스트 |
| `mysql-integration` | `./gradlew mysqlIntegrationTest` | 실제 DB 필요한 통합 테스트 |

---

## 주요 환경 설정

| 항목 | 위치 |
|---|---|
| DB 연결 등 로컬 설정 | `application/src/main/resources/application-local.yml` (gitignore) |
| 출석 정책 설정 | `AttendancePolicyProperties` (application.yml 바인딩) |
| 빌드 의존성 버전 | `gradle/libs.versions.toml` |
