---
name: hexagonal-arch
description: dpm-core-server 레이어 의존 방향, Port 네이밍 규칙(PersistencePort/QueryUseCase/CommandUseCase), 위반 패턴 목록.
---

# 헥사고날 아키텍처 규칙

## 모듈 의존 방향

실제 `build.gradle.kts` 기준 의존 관계:

```
entity ──▶ persistence ──▶ domain
                             ▲
                             │
          application ───────┘
             │
             └──▶ persistence   (DI 조립용)
```

- `domain` 은 어디에도 의존하지 않는다.
- `persistence` 는 `domain` + `entity` 에 의존.
- `application` 은 `domain` + `persistence` 에 의존 (실제 구현체 주입을 위해).
- `entity` 는 JPA Entity 정의만 포함, 다른 모듈 의존 없음.

위반 예시:
- `domain`이 `application`을 import → 역방향 의존
- `application` 코드에서 `core.persistence.*` 패키지의 **구현체 클래스**를 직접 import → Port 인터페이스로만 접근해야 함
- `application`이 JPA `@Entity` 클래스를 직접 참조 → aggregate 변환 레이어에서만 다뤄야 함

---

## 레이어별 허용 / 금지

### domain

| 허용 | 금지 |
|---|---|
| 순수 Kotlin (aggregate, vo, enum, port 인터페이스) | `@Service`, `@Repository`, `@Transactional` |
| Port 인터페이스 정의 | Spring Framework import |
| 도메인 예외 | JPA `@Entity` |

### application

| 허용 | 금지 |
|---|---|
| `@Service`, `@Transactional` | JPA Entity 직접 참조 (`@Entity` import) |
| Port 인터페이스(domain) 주입 | persistence 패키지 직접 import |
| Controller (`@RestController`, `@PreAuthorize`) | jOOQ 생성 테이블 클래스 직접 참조 |
| 응답 DTO (`*Response`, `*Request`) | |

### persistence

| 허용 | 금지 |
|---|---|
| Port 인터페이스 구현체 | domain 비즈니스 로직 |
| jOOQ, Spring Data JPA | |
| Entity ↔ Domain 변환 | |

---

## Port 네이밍 규칙

```
outbound (persistence → domain 방향):
  *PersistencePort
  예: AttendancePersistencePort, SessionPersistencePort

inbound (application → domain 방향):
  *QueryUseCase    — 조회 전용
  *CommandUseCase  — 상태 변경
  예: AttendanceQueryUseCase, SessionCommandUseCase
```

Port 파일 위치:
```
domain/src/main/kotlin/core/domain/<도메인>/port/
  outbound/   ← *PersistencePort
  inbound/
    query/    ← *QueryUseCase
    command/  ← *CommandUseCase
```

---

## 실제 예시

```kotlin
// domain/attendance/port/outbound/AttendancePersistencePort.kt
interface AttendancePersistencePort {
    fun findBySessionIdAndMemberId(sessionId: Long, memberId: Long): Attendance?
    fun save(attendance: Attendance): Attendance
}

// application/attendance/application/service/AttendanceCommandService.kt
@Service
class AttendanceCommandService(
    private val attendancePersistencePort: AttendancePersistencePort,  // ← Port만 의존
    private val sessionPersistencePort: SessionPersistencePort,
    ...
)

// persistence/attendance/.../AttendancePersistenceAdapter.kt
@Component
class AttendancePersistenceAdapter(
    private val attendanceRepository: AttendanceJpaRepository,
) : AttendancePersistencePort {  // ← Port 구현
    override fun save(attendance: Attendance): Attendance { ... }
}
```

---

## 자주 발생하는 위반 패턴

| 위반 | 증상 | 수정 |
|---|---|---|
| application에서 JPA entity import | `import core.entity.*` in service | Port 메서드 반환 타입을 domain aggregate로 변경 |
| domain에 `@Transactional` | 테스트에서 트랜잭션 없어도 동작, 프로덕션에서만 문제 | domain에서 제거, application service로 이동 |
| persistence에서 비즈니스 로직 | Adapter 클래스에 if 분기 증가 | domain aggregate 메서드로 이동 |

---

## 도메인 패키지 목록 (19기 기준)

`absencereason`, `afterParty`, `announcement`, `attendance`, `authorization`, `bill`, `cohort`, `gathering`, `image`, `member`, `membercredential`, `notification`, `refreshToken`, `security`, `session`, `sessionFeedback`, `team`

신규 도메인 추가 시 위 구조 그대로 패키지 생성.
