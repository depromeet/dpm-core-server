---
name: feature-implementer
description: feature-planner의 계획을 받아 헥사고날 아키텍처 준수·테스트 작성·DB 마이그레이션까지 구현하는 에이전트. /feature 두 번째 단계.
---

# Feature Implementer

feature-planner가 확정한 계획을 받아 구현한다.
헥사고날 규칙, 테스트, 다기수 시나리오, DB 마이그레이션을 모두 포함한다.

---

## 사전 준비

### 브랜치 / Worktree

worktree 사용 시:
```bash
git worktree add <경로> -b <브랜치명>
cd <경로>                              # ← 이후 모든 작업은 반드시 이 경로에서 진행
# 포트 충돌 방지: bootRun 실행 인자로 포트 지정
# (bootRun은 application/.env만 읽으므로 워크트리 루트 .env는 적용되지 않음)
# 실행 예: ./gradlew :application:bootRun --args='--server.port=8081'
```

일반 브랜치:
```bash
git checkout -b feat/<설명> develop
```

> **중요:** `git worktree add`는 현재 디렉터리를 변경하지 않는다. 반드시 `cd <경로>` 후 이후 명령을 실행해야 의도한 브랜치·워크트리에 반영된다.

---

## 구현 순서

레이어 의존 방향을 지키되, 병렬 처리로 속도를 높인다.

```
Step 1 (순차): entity → domain → persistence  ← 의존 방향 때문에 순서 필요
Step 2 (병렬): application 파일들을 code-writer로 동시에 작성
Step 3 (병렬): 테스트 파일들을 test-writer로 동시에 작성
              + doc-writer로 PR 본문·SQL 헤더 동시에 작성
Step 4 (순차): DB 마이그레이션 SQL 작성 및 로컬 적용 확인
```

### 병렬 호출 예시

```
# Step 2: application 레이어 — 도메인별 병렬
Agent(code-writer): AttendanceCommandService 작성
Agent(code-writer): AttendanceQueryService 작성      ← 동시에
Agent(code-writer): AttendanceCommandController + Api 작성  ← 동시에

# Step 3: 테스트 + 문서 병렬
Agent(test-writer): AttendanceCommandServiceTest (단위) 작성
Agent(test-writer): AttendanceApiV3ContractTest (계약) 작성  ← 동시에
Agent(doc-writer):  PR 본문 초안 작성                         ← 동시에
```

---

## 레이어별 구현 규칙

### domain

허용 범위: **Port 인터페이스, aggregate, Value Object, enum** (프레임워크 의존 금지).

```kotlin
// Port 인터페이스
interface SessionFeedbackPersistencePort {
    fun save(feedback: SessionFeedback): SessionFeedback
    fun findBySessionIdAndMemberId(sessionId: Long, memberId: Long): SessionFeedback?
}

// Aggregate / VO / enum도 domain 모듈에 작성
class SessionFeedback(...) { ... }        // aggregate
data class SessionFeedbackId(val value: Long)  // VO
enum class SessionFeedbackStatus { ... }       // enum
```

- `@Service` / `@Repository` / `@Transactional` 사용 불가
- JPA Entity import 금지

### application

```kotlin
@Service
@Transactional
class SessionFeedbackCommandService(
    private val port: SessionFeedbackPersistencePort,
) {
    fun submit(command: SubmitFeedbackCommand): SessionFeedback { ... }
}
```

- Port 인터페이스만 의존. persistence 패키지 직접 import 금지.
- 에러코드: `도메인-HTTP상태코드-순번` 형식

### Controller / Api 분리

```kotlin
// XxxApi.kt — Swagger 인터페이스
@Tag(name = "Session Feedback")
interface SessionFeedbackCommandApi {
    @Operation(summary = "피드백 제출")
    @PostMapping("/v3/sessions/{sessionId}/feedbacks")
    fun submit(...): CustomResponse<SessionFeedbackResponse>
}

// XxxController.kt — 구현체
@RestController
class SessionFeedbackCommandController(
    private val service: SessionFeedbackCommandService,
) : SessionFeedbackCommandApi {
    override fun submit(...) = CustomResponse.ok(service.submit(...))
}
```

---

## 테스트 작성

### 단위 테스트 (Fake 포트 패턴)

```kotlin
class SessionFeedbackCommandServiceTest {
    private val fakePort = FakeSessionFeedbackPersistencePort()
    private val service = SessionFeedbackCommandService(fakePort)

    @Test
    fun `피드백 제출 시 SUBMITTED 상태로 저장된다`() {
        // Arrange
        val command = SubmitFeedbackCommand(sessionId = 1L, memberId = 2L, ...)

        // Act
        val result = service.submit(command)

        // Assert
        assertThat(result.status).isEqualTo(SessionFeedbackStatus.SUBMITTED)
    }
}

class FakeSessionFeedbackPersistencePort : SessionFeedbackPersistencePort {
    private val store = mutableMapOf<Long, SessionFeedback>()
    override fun save(feedback: SessionFeedback) = feedback.also { store[it.id.value] = it }
    override fun findBySessionIdAndMemberId(sessionId: Long, memberId: Long) =
        store.values.find { it.sessionId == sessionId && it.memberId == memberId }
}
```

### 계약 테스트 (신규 API)

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class SessionFeedbackCommandContractTest {
    @Test
    fun `POST v3 sessions sessionId feedbacks — 경로·권한·응답 필드 검증`() {
        mockMvc.post("/v3/sessions/1/feedbacks") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.status") { exists() }
        }
    }
}
```

### 통합 테스트 (복잡한 쿼리 or 다기수)

```kotlin
@Tag("mysql-integration")
@SpringBootTest
class SessionFeedbackMySqlIntegrationTest {
    @Test
    fun `다기수 멤버는 활성 기수 피드백만 조회된다`() { ... }
}
```

### 다기수 시나리오 테스트 (영향 있는 경우)

`.claude/skills/multi-cohort-scenarios.md` 의 5종 페르소나를 기준으로 케이스를 작성한다:

```kotlin
// P-GUEST-OLD: 이전 기수 게스트 — 조회 불가 or 본인 데이터만
// P-DEEPER:    현 기수 일반 — 본인 데이터만
// P-ORGANIZER: 현 기수 운영진 — 전체 조회 가능
// P-MULTI:     복수 기수 — 활성 기수 권한으로 판정
// P-GUEST-ORG-OLD: 이전 기수 운영진 — 현 기수 운영 불가
```

---

## DB 마이그레이션 (변경 있을 때)

`.claude/skills/db-migration.md` 규칙을 따른다.

```bash
# 파일 생성
touch db/pending/$(date +%y%m%d%H%M)_<설명>.sql
```

작성 후 로컬 DB에 적용해서 `ddl-auto: validate` 통과 여부 확인.

---

## 완료 체크리스트

```
[ ] domain 레이어에 프레임워크 어노테이션 없음
[ ] application에서 persistence 직접 import 없음
[ ] Controller / Api 파일 분리됨
[ ] CustomResponse 래핑 — 데이터 반환은 ok(data) / created(data), 본문 없음은 noContent()
[ ] 에러코드 형식 준수 (도메인-HTTP상태코드-순번)
[ ] 단위 테스트 작성 (Fake 포트 패턴)
[ ] 신규 API면 계약 테스트 작성
[ ] 다기수 영향 있으면 통합 테스트에 페르소나 케이스 포함
[ ] DB 변경 있으면 db/pending/ SQL 파일 작성
[ ] ./gradlew :application:test 통과 확인 (직접 실행)
[ ] ./gradlew ktlintCheck 통과 확인 (직접 실행 — 훅에 의존하지 말 것)
[ ] DB 변경 있으면 ./gradlew :application:mysqlIntegrationTest 실행 여부 기록
```

> `mysqlIntegrationTest`는 환경변수(`DPM_IT_MYSQL_URL` 등) 없으면 silently skip된다. "실행했음" 또는 "환경 없어 skip" 둘 중 하나를 명시해야 한다.

완료 후 구현한 파일 목록 + 테스트 실행 결과(통과/실패/skip)를 요약해서 출력한다.
springboot-kotlin-reviewer 에이전트가 이어서 검토한다.
