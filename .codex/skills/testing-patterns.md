---
name: testing-patterns
description: dpm-core-server 3종 테스트 패턴(단위/통합/계약), TestFixture·Fake 포트·MutableClock 사용법, 다기수 시나리오 체크리스트.
---

# Testing Patterns — dpm-core-server

이 프로젝트의 테스트는 3종으로 나뉜다. 각 종류마다 목적과 작성 위치가 다르다.

---

## 테스트 3종 개요

| 종류 | 패키지 | 실행 조건 | Spring 컨텍스트 |
|---|---|---|---|
| 단위 | `core.application.*.*Test` | 항상 | 없음 |
| 통합 | `core.it.*.*MySqlIntegrationTest` | 환경변수 필요 | `@SpringBootTest` |
| 계약 | `core.application.*.*ContractTest` | 항상 | 없음 |

---

## 1. 단위 테스트

### 핵심 원칙
- **Spring 컨텍스트 없음.** `@SpringBootTest` 사용 금지.
- **Fake 포트 사용.** `@MockitoBean`이나 `Mockito.mock()` 남발 금지.
  - Mockito는 진짜 외부 의존성(`MemberQueryUseCase` 등)에만 쓴다.
- **TestFixture 패턴.** 여러 테스트 클래스가 같은 도메인을 다루면 `*TestFixture`로 조립 공유.

### TestFixture 패턴

Fake 포트 + 실제 서비스를 한 곳에서 조립한다. 각 테스트 클래스는 `TestFixture`를 필드로 갖는다.

```kotlin
// 사용 예
class AttendanceQueryServiceTest {
    private val now = Instant.parse("2026-10-10T03:00:00Z")
    private val fixture = AttendanceTestFixture(now = now)

    @Test
    fun `현재 기수 세션은 전체 명단을 반환한다`() {
        val cohortId = fixture.createActiveCohort()
        val session = fixture.createSession(cohortId, times)
        // fixture.attendances, fixture.sessions 직접 조작
        val result = fixture.attendanceQueryService.getSessionRoster(session.id!!, adminId)
        assertThat(result.members).hasSize(28)
    }
}
```

기존 Fixture:
- `AttendanceTestFixture` — 출석/세션/기수 관련 서비스 조립
- `FakeAttendancePersistencePort`, `FakeSessionPersistencePort`, `FakeCohortPersistencePort`
- `FakeSessionFeedbackFormPersistencePort`, `FakeSessionFeedbackPushReceiptPersistencePort`

### Fake 포트 작성 규칙

```kotlin
class FakeXxxPersistencePort : XxxPersistencePort {
    // 1. 인메모리 저장소 — linkedMapOf 사용 (삽입 순서 보장)
    private val rows = linkedMapOf<Long, Row>()
    private val sequence = AtomicLong(0)

    // 2. 모든 메서드 @Synchronized (단위 테스트에서 동시성 버그 숨김 방지)
    @Synchronized
    override fun save(entity: Xxx): Xxx { ... }

    // 3. SQL 의존 복잡 조회는 UnsupportedOperationException
    // → MySQL 통합 테스트에서 검증한다는 주석 명시
    override fun findComplexQuery(...): List<Xxx> =
        throw UnsupportedOperationException()

    // 4. 테스트 전용 헬퍼 — 도메인 메서드가 아닌 것
    fun insert(...): Long { ... }
    fun all(): List<Row> = rows.values.map { it.copy() }
}
```

### MutableClock — 시간 의존 테스트

`Clock`을 주입받는 서비스는 `MutableClock`으로 시간을 제어한다.

```kotlin
val clock = MutableClock(Instant.parse("2026-10-10T18:00:00Z"))

// 시간 이동
clock.now = clock.now.plus(Duration.ofMinutes(16))  // 지각 구간으로

val result = service.attend(command)
assertThat(result.status).isEqualTo(AttendanceStatus.LATE)
```

### 테스트 네이밍

백틱 한국어 설명 사용. "무엇을 했을 때 어떻게 된다" 형식.

```kotlin
@Test
fun `인증 마감 이후 출석 시도는 거절된다`() { ... }

@Test
fun `없거나 삭제됐거나 현재 활성 기수가 아닌 세션의 명단은 찾을 수 없다`() { ... }
```

---

## 2. MySQL 통합 테스트

### 목적
단위 Fake로 검증할 수 없는 것:
- 조건부 UPDATE (`recordAttendanceIfAllowed` 등 경쟁 조건)
- jOOQ/JPQL 복잡 쿼리 정확성
- DB 잠금 동작 (`SELECT FOR UPDATE`)
- 동시 요청 처리 (동시성 버그)

### 선언

```kotlin
@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = "DPM_IT_MYSQL_URL", matches = ".+")
@SpringBootTest(
    classes = [XxxMySqlIntegrationTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
class XxxMySqlIntegrationTest {
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    // ...
}
```

### 실행 방법

```bash
# 실제 연결 정보는 CLAUDE.local.md 참고
DPM_IT_MYSQL_URL='...' \
DPM_IT_MYSQL_USERNAME='...' \
DPM_IT_MYSQL_PASSWORD='...' \
./gradlew :application:mysqlIntegrationTest
```

기본 `./gradlew test`에 포함되지 않는다. CI에서 별도 job으로 실행.

### 동시성 테스트 패턴

```kotlin
val latch = CountDownLatch(threadCount)
val executor = Executors.newFixedThreadPool(threadCount)

val futures = (1..threadCount).map {
    executor.submit(Callable {
        latch.countDown()
        latch.await()   // 동시 출발
        service.attend(command)
    })
}

val results = futures.map { runCatching { it.get(5, TimeUnit.SECONDS) } }
assertThat(results.count { it.isSuccess }).isEqualTo(1)   // 한 명만 성공
```

---

## 3. 계약 테스트

### 목적
API 경로, HTTP 메서드, 권한 표현식이 의도와 다르게 변경되는 것을 방지.

스택 PR(API 경로 변경)이 있을 때 반드시 계약 테스트를 함께 업데이트한다.

### 패턴

```kotlin
class AttendanceApiV3PathContractTest {
    private fun endpoints(vararg controllers: Class<*>): Set<String> =
        controllers.flatMap { controller ->
            controller.declaredMethods
                .filterNot { it.isSynthetic || it.isBridge }
                .mapNotNull { method ->
                    val mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java)
                        ?: return@mapNotNull null
                    val authority = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize::class.java)!!.value
                    "${mapping.method.single()} ${mapping.path.firstOrNull().orEmpty()} $authority"
                }
        }.toSet()

    @Test
    fun `출석 API 경로와 권한은 고정이다`() {
        assertThat(endpoints(AttendanceCommandController::class.java))
            .containsExactlyInAnyOrder(
                "POST /v1/sessions/{sessionId}/attendances hasAuthority('create:attendance')",
                // ...
            )
    }
}
```

새 엔드포인트 추가 시 → 계약 테스트 목록에 추가.  
경로/권한 변경 시 → 계약 테스트 함께 수정.

---

## 금지 사항

| 금지 | 이유 |
|---|---|
| 단위 테스트에서 `@SpringBootTest` | 컨텍스트 로딩으로 수백 ms 낭비 |
| Fake 대신 `@MockitoBean` 남발 | 구현 없는 목 → 행동 검증 불가 |
| 통합 테스트에서 `assumeTrue` 없이 환경변수 의존 | CI에서 조건 없이 실패 |
| 단위 테스트에서 Thread.sleep | 불확정 타이밍 → `MutableClock`으로 대체 |
| `UnsupportedOperationException` 없이 복잡 쿼리 Fake 구현 | 버그 숨김 |

---

## 새 도메인 테스트 추가 시 체크리스트

- [ ] `Fake*PersistencePort` 작성 — 모든 메서드 `@Synchronized`
- [ ] `*TestFixture`에 Fake 추가 또는 신규 Fixture 작성
- [ ] SQL 의존 메서드는 `UnsupportedOperationException` + 주석
- [ ] 시간 의존 로직은 `MutableClock` 주입
- [ ] API 경로 추가 시 `*ContractTest` 목록 업데이트
- [ ] 동시성 버그 가능성 있으면 MySQL 통합 테스트 추가

---

## 4. 다기수·다역할 시나리오 검토

새 API를 작성하거나 기존 API를 수정할 때 아래 페르소나 조합을 기준으로 영향도를 점검한다.

### 권한 판정 원리

JWT `permissions` 배열은 **role.cohortId == 현재 활성 기수 ID** 인 역할만 포함된다.
(`CurrentCohortRoleResolver.isAssignmentEffective()`)

```
이전 기수 역할 → 현재 기수에서 무효 → permissions 배열 비어있음 → GUEST 취급
```

역할 판별 기준:
- **GUEST**: permissions 배열 비어있음 (또는 `isAuthenticated()` 만 통과)
- **DEEPER**: permissions 7개, `update:attendance` 없음
- **ORGANIZER**: permissions 18개, `update:attendance` 있음

### 표준 페르소나 5종

| ID | 설명 | DB 역할 | 현재 기수 유효 역할 | JWT permissions |
|---|---|---|---|---|
| **P-GUEST-OLD** | 이전 기수 멤버 (예: 17기 DEEPER) | 17기 DEEPER | GUEST | 비어있음 |
| **P-GUEST-ORG-OLD** | 이전 기수 운영진 (예: 18기 ORGANIZER) | 18기 ORGANIZER | GUEST | 비어있음 |
| **P-DEEPER** | 현재 기수 일반 멤버 | 19기 DEEPER | DEEPER | 7개 (`update:attendance` 없음) |
| **P-ORGANIZER** | 현재 기수 운영진 | 19기 ORGANIZER | ORGANIZER | 18개 (`update:attendance` 있음) |
| **P-MULTI** | 이전·현재 기수 동시 소속 (예: 17기+19기 DEEPER) | 17기 DEEPER + 19기 DEEPER | DEEPER | 7개 (19기 역할만 적용) |

> **주의**: `ACTIVE` 상태 전환 시 `initializeMemberDataForActiveMember()`가 현재 활성 기수에 자동 소속시킨다.
> "이전 기수만 소속" 시나리오는 일반 API로 재현 불가 — DB 직접 조작 필요.

### 권한별 페르소나 매트릭스

| 권한 | P-GUEST-OLD | P-GUEST-ORG-OLD | P-DEEPER | P-ORGANIZER | P-MULTI |
|---|:---:|:---:|:---:|:---:|:---:|
| `isAuthenticated()` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `read:session` | ❌ 403 | ❌ 403 | ✅ | ✅ | ✅ |
| `create:attendance` | ❌ 403 | ❌ 403 | ✅ | ✅ | ✅ |
| `read:attendance` | ❌ 403 | ❌ 403 | ✅ | ✅ | ✅ |
| `update:attendance` | ❌ 403 | ❌ 403 | ❌ 403 | ✅ | ❌ 403 |
| `update:member` | ❌ 403 | ❌ 403 | ❌ 403 | ✅ | ❌ 403 |
| `create:session` | ❌ 403 | ❌ 403 | ❌ 403 | ✅ | ❌ 403 |

### API 카테고리별 필수 체크포인트

**`isAuthenticated()` — 로그인 여부만 확인**
- GUEST도 접근 가능 → 응답에 현재 기수 데이터가 없는 케이스를 처리했는지 확인

**`read:session` / `create:attendance` — DEEPER 이상**
- GUEST에게 403 대신 의미있는 응답이 필요한 API인가? (예: 피드백 `NOT_TARGET` 반환 고려)
- P-MULTI 응답이 현재 기수 데이터만 담고 있는가?

**`update:attendance` / `update:member` — ORGANIZER 전용**
- P-GUEST-ORG-OLD(이전 기수 운영진)이 403인가? 반드시 확인

**출석 조회 — 기수 필터링**
- `findAllCohortSessions(activeCohortId)` 또는 활성 기수 필터가 걸려 있는가?
- P-MULTI 응답에 이전 기수 데이터가 섞이지 않는가?

**세션 피드백**
```
P-GUEST-OLD               → 403 (NOT_TARGET이 아님, 앱 클라이언트 처리 필요)
P-DEEPER (출석=PENDING)   → ATTENDANCE_INCOMPLETE
P-DEEPER (출석=LATE)      → AVAILABLE 또는 SUBMITTED
P-DEEPER (출석 없음)      → NOT_TARGET
P-DEEPER (form 없음)      → NOT_TARGET (startAt=null)
```

**홈 화면 / 넥스트 세션**
```
P-GUEST-OLD / P-GUEST-ORG-OLD → COHORT_ENDED (latestCohortId != activeCohortId)
P-DEEPER / P-ORGANIZER / P-MULTI → AVAILABLE (세션 있을 때)
```

### 새 API 작성 시 다기수 체크리스트

- [ ] 사용하는 `@PreAuthorize` 권한으로 위 매트릭스에서 각 페르소나 결과 확인
- [ ] GUEST(이전 기수 멤버) 접근 시 403이 의도된 동작인가, 별도 응답이 필요한가?
- [ ] P-GUEST-ORG-OLD(이전 기수 운영진)이 운영진 전용 API에서 403인가?
- [ ] P-MULTI 응답이 현재 기수 데이터만 담고 있는가?
- [ ] 출석 상태 의존 API라면 PENDING 케이스를 처리했는가?
- [ ] 피드백 관련 API라면 form=null(피드백 미개설) 케이스를 처리했는가?

### Dev 환경 테스트 계정

> 계정 정보(이메일, 비밀번호, memberId)는 `CLAUDE.local.md` 참고.

### 과거 발견 버그 패턴

| 증상 | 원인 | 확인 포인트 |
|---|---|---|
| 이전 기수 운영진이 현재 기수 API 접근 성공 | `isAuthenticated()`만 사용 | `@PreAuthorize` 권한 표현식 확인 |
| P-MULTI 출석 조회에 이전 기수 세션 포함 | cohortId 필터 누락 | 쿼리의 cohortId 조건 확인 |
| GUEST 피드백 API → 403 (NOT_TARGET 아님) | `read:session` 권한 필요 | 앱 클라이언트 403 처리 여부 확인 |
| 수동 LATE 유저 피드백 푸시 미발송 | scheduler `attendedAt == null` 조건 | `SessionFeedbackReminderScheduler` 참고 |
