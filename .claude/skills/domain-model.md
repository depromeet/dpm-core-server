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
PENDING, ABSENT, LATE → EXCUSED_ABSENT (결석 사유서 제출 → 즉시 전환)
EXCUSED_ABSENT → (유지)  반려해도 출석 상태 유지, 사유서 상태만 REJECTED
```

| 값 | 설명 |
|---|---|
| `PENDING` | 미출석 (기본값) |
| `PRESENT` | 출석 완료 |
| `LATE` | 지각 |
| `ABSENT` | 결석 (자동 또는 수동) |
| `EXCUSED_ABSENT` | 인정 결석 |

**결석 사유서 플로우**:
1. 멤버 제출 → 즉시 `EXCUSED_ABSENT` 전환
2. 운영진 반려 → 사유서 상태만 `REJECTED`, 출석은 `EXCUSED_ABSENT` 유지 (주의)
3. 인정 취소하려면: 멤버가 직접 삭제(`releaseExcusedAbsence`) 또는 운영진 수동 변경

---

## 세션 피드백 상태 (SessionFeedbackMyStatus) — 멤버 뷰

| 값 | 조건 |
|---|---|
| `NOT_TARGET` | 출석 명단 없음 / 피드백 form 미개설(startAt=null) |
| `ATTENDANCE_INCOMPLETE` | 출석 명단 있으나 `PENDING` |
| `AVAILABLE` | 출석(PRESENT/LATE) + form 진행 중 + 미제출 |
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

## 권한 판정 (JWT permissions)

JWT `permissions` 배열 = **activeCohortId 역할만** 포함 (`CurrentCohortRoleResolver`).

| 역할 | 조건 |
|---|---|
| `GUEST` | permissions 비어있음 (이전 기수 멤버 포함) |
| `DEEPER` | permissions 7개, `update:attendance` 없음 |
| `ORGANIZER` | permissions 18개, `update:attendance` 있음 |

---

## 다기수 멤버 (Multi-cohort)

이전 기수 + 현재 기수 동시 소속. JWT에는 현재 기수 역할만 포함.  
→ 자세한 시나리오·테스트 계정: `.claude/skills/testing-patterns.md` 섹션 4

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
