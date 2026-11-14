---
name: domain-model
description: dpm-core-server 도메인 용어·상태값. 출석/피드백 상태 전이, 권한 판정 원리, 결석사유서 플로우, 주요 VO.
---

# 도메인 모델 · 용어집

## 기수 (Cohort)

- 활동 주기 단위. 문자열 `value` 로 저장 (`"17"`, `"18"`, `"19"`, `"코어 1기"` 등).
- **활성 기수 (activeCohort)**: `isActive = true` 인 기수. 항상 1개.
- 테스트 고정 ID 매핑: 17기=1, 18기=2, 19기=3 (실 환경 추정, DB 직접 확인 필요).
- `ACTIVE` 상태 전환 시 `initializeMemberDataForActiveMember()` 가 현재 기수에 자동 소속.

---

## 출석 상태 (AttendanceStatus)

```
PENDING → PRESENT    (출석 코드 인증)
PENDING → LATE       (지각 코드 인증 / 운영진 수동)
PENDING → ABSENT     (자동 결석 처리 / 운영진 수동)
PENDING, ABSENT, LATE → EXCUSED_ABSENT (운영진이 사유서 승인)
검토 대기 중 미출석 → 19시 배치에서 ABSENT, 이후 승인되면 EXCUSED_ABSENT
```

| 값 | 설명 |
|---|---|
| `PENDING` | 미출석 (기본값) |
| `PRESENT` | 출석 완료 |
| `LATE` | 지각 |
| `ABSENT` | 결석 (자동 또는 수동) |
| `EXCUSED_ABSENT` | 인정 결석 |

**결석 사유서 플로우**:
1. 디퍼 제출·재제출 → 사유서 `PENDING`, 출석은 그대로
2. 운영진 승인 → 사유서 `APPROVED` + 출석 `EXCUSED_ABSENT` (한 트랜잭션)
3. 운영진 반려 → 사유서 `REJECTED`, 출석은 그대로. 디퍼가 다시 제출할 수 있음
4. 디퍼는 `PENDING`·`REJECTED` 사유서만 수정·삭제할 수 있음 (`APPROVED`는 불가)

---

## 세션 피드백 상태 (SessionFeedbackMyStatus) — 멤버 뷰

| 값 | 조건 |
|---|---|
| `NOT_TARGET` | 출석 명단 없음 / 피드백 form 미개설(startAt=null) |
| `ATTENDANCE_INCOMPLETE` | 출석 명단 있으나 `PENDING` |
| `AVAILABLE` | 출석(PRESENT/LATE) 또는 미인정 출석(ABSENT + `lateParticipatedAt != null`) + form 진행 중 + 미제출 |
| `BEFORE_START` | form 개설됐으나 아직 시작 전 |
| `SUBMITTED` | 이미 제출 완료 |
| `EXPIRED` | form 종료 후 미제출 |

---

## 피드백 폼 상태 (SessionFeedbackStatus) — 폼 자체

| 값 | 조건 |
|---|---|
| `SCHEDULED` | `now < startAt` |
| `IN_PROGRESS` | `startAt <= now < endAt` |
| `CLOSED` | `now >= endAt` |

---

## 권한 판정 (매 요청 DB 조회)

권한은 JWT claim이 아니라 **매 요청마다 DB에서 조회**한다 (`JwtTokenProvider.getAuthentication()`).

```kotlin
// JwtTokenProvider.kt
val permissions = roleQueryUseCase
    .getPermissionsByMemberId(MemberId(memberId))
    .map { SimpleGrantedAuthority(it) }
```

`RoleQueryUseCase.getPermissionsByMemberId()` 는 **activeCohortId 역할만** 조회한다 (`CurrentCohortRoleResolver`). JWT는 memberId만 운반하고, 권한은 서버가 매 요청 계산한다.

| 역할 | 조건 |
|---|---|
| `GUEST` | permissions 비어있음 (이전 기수 멤버 포함) |
| `DEEPER` | permissions 7개, `update:attendance` 없음 |
| `ORGANIZER` | permissions 18개, `update:attendance` 있음 |

---

## 다기수 멤버 (Multi-cohort)

이전 기수 + 현재 기수 동시 소속. JWT에는 현재 기수 역할만 포함.
→ 자세한 시나리오·테스트 계정: `.agents/skills/testing-patterns/SKILL.md` 섹션 4

---

## 세션 출석 타임라인

```
attendanceStart → 출석 가능
lateStart       → 지각 처리 시작
absentStart     → 결석 자동 전환
```

---

## QA 계정

- 도메인: `@dpm.test`
- 집계·목록 쿼리에서 제외. 개인 조회(로그인·마이페이지)는 포함.

---

## 주요 VO

| VO | 타입 | 설명 |
|---|---|---|
| `CohortId` | value: Long | 기수 ID |
| `SessionId` | value: Long | 세션 ID |
| `MemberId` | value: Long | 멤버 ID |
| `AttendanceId` | value: Long | 출석 ID |
| `TeamId` | value: Long | 팀 ID |
