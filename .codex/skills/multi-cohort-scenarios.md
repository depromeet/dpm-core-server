---
name: multi-cohort-scenarios
description: 다기수·다역할 API 영향도 검토. 5종 페르소나(P-GUEST-OLD/P-DEEPER/P-ORGANIZER/P-MULTI), 권한 매트릭스, dev 테스트 계정.
---

# 다기수·다역할 시나리오 검토 스킬

새 API를 작성하거나 기존 API를 수정할 때 아래 페르소나 조합을 기준으로 영향도를 점검한다.

---

## 권한 판정 원리

JWT `permissions` 배열은 **role.cohortId == 현재 활성 기수 ID** 인 역할만 포함된다.
(`CurrentCohortRoleResolver.isAssignmentEffective()`)

```
이전 기수 역할 → 현재 기수에서 무효 → permissions 배열 비어있음 → GUEST 취급
```

역할 판별 기준:
- **GUEST**: permissions 배열 비어있음 (또는 `isAuthenticated()` 만 통과)
- **DEEPER**: permissions 7개, `update:attendance` 없음
- **ORGANIZER**: permissions 18개, `update:attendance` 있음

---

## 표준 페르소나 5종

| ID | 설명 | DB 역할 | 현재 기수 유효 역할 | JWT permissions |
|---|---|---|---|---|
| **P-GUEST-OLD** | 이전 기수 멤버 (예: 17기 DEEPER) | 17기 DEEPER | GUEST | 비어있음 |
| **P-GUEST-ORG-OLD** | 이전 기수 운영진 (예: 18기 ORGANIZER) | 18기 ORGANIZER | GUEST | 비어있음 |
| **P-DEEPER** | 현재 기수 일반 멤버 | 19기 DEEPER | DEEPER | 7개 (`update:attendance` 없음) |
| **P-ORGANIZER** | 현재 기수 운영진 | 19기 ORGANIZER | ORGANIZER | 18개 (`update:attendance` 있음) |
| **P-MULTI** | 이전·현재 기수 동시 소속 (예: 17기+19기 DEEPER) | 17기 DEEPER + 19기 DEEPER | DEEPER | 7개 (19기 역할만 적용) |

> **주의**: `ACTIVE` 상태 전환 시 `initializeMemberDataForActiveMember()`가 현재 활성 기수에 자동 소속시킨다.
> "이전 기수만 소속" 시나리오는 일반 API로 재현 불가 — DB 직접 조작 필요.

---

## 권한별 페르소나 매트릭스

| 권한 | P-GUEST-OLD | P-GUEST-ORG-OLD | P-DEEPER | P-ORGANIZER | P-MULTI |
|---|:---:|:---:|:---:|:---:|:---:|
| `isAuthenticated()` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `read:session` | ❌ 403 | ❌ 403 | ✅ | ✅ | ✅ |
| `create:attendance` | ❌ 403 | ❌ 403 | ✅ | ✅ | ✅ |
| `read:attendance` | ❌ 403 | ❌ 403 | ✅ | ✅ | ✅ |
| `update:attendance` | ❌ 403 | ❌ 403 | ❌ 403 | ✅ | ❌ 403 |
| `update:member` | ❌ 403 | ❌ 403 | ❌ 403 | ✅ | ❌ 403 |
| `create:session` | ❌ 403 | ❌ 403 | ❌ 403 | ✅ | ❌ 403 |

---

## API 카테고리별 필수 검토 조합

### 1. 인증 없이 접근 가능한 API (`permitAll`)
검토 불필요. 모든 페르소나 동일.

---

### 2. `isAuthenticated()` — 로그인 여부만 확인
```
P-GUEST-OLD  → ✅ (이전 기수도 로그인 가능)
P-DEEPER     → ✅
P-ORGANIZER  → ✅
```
**주의**: GUEST도 접근 가능한 API라면, 응답에 현재 기수 데이터가 없는 케이스를 처리했는지 확인.

---

### 3. `read:session` / `create:attendance` — DEEPER 이상
```
P-GUEST-OLD     → 403  ← 이 케이스가 올바른 UX인지 확인
P-GUEST-ORG-OLD → 403  ← 이전 기수 운영진도 403, 의도된 동작인지
P-DEEPER        → 200
P-ORGANIZER     → 200
P-MULTI         → 200 (19기 역할 유효)
```
**체크포인트**:
- GUEST에게 403 대신 의미있는 응답이 필요한 API인가? (예: 피드백 `NOT_TARGET` 반환 고려)
- P-MULTI의 응답이 현재 기수 데이터만 담고 있는가?

---

### 4. `update:attendance` / `update:member` — ORGANIZER 전용
```
P-GUEST-OLD     → 403
P-GUEST-ORG-OLD → 403  ← 핵심: 이전 기수 운영진은 현재 기수 데이터 수정 불가
P-DEEPER        → 403
P-ORGANIZER     → 200
P-MULTI         → 403  ← 이전+현재 DEEPER도 운영진 권한 없음
```
**체크포인트**:
- P-GUEST-ORG-OLD(이전 기수 운영진)이 403인가? 반드시 확인.

---

### 5. 출석 조회 API (기수 필터링)
```
P-MULTI → 현재 기수(19기) 세션 N개만 반환, 이전 기수(17기) 혼합 없음
```
**체크포인트**:
- `findAllCohortSessions(activeCohortId)` 또는 활성 기수 필터를 사용하고 있는가?
- P-MULTI 응답에 이전 기수 데이터가 섞이지 않는가?

---

### 6. 세션 피드백 API
```
P-GUEST-OLD  → 403 (read:session 없음) — NOT_TARGET이 아닌 403
P-DEEPER (출석=PENDING)   → ATTENDANCE_INCOMPLETE
P-DEEPER (출석=LATE)      → AVAILABLE 또는 SUBMITTED (form 개설 여부에 따라)
P-DEEPER (출석 없음)      → NOT_TARGET
P-DEEPER (피드백 form 없음) → NOT_TARGET (startAt=null)
```
**체크포인트**:
- 피드백 form이 없는 세션에서 NOT_TARGET이 올바르게 반환되는가?
- GUEST의 403을 앱 클라이언트가 NOT_TARGET으로 처리하고 있는가?

---

### 7. 홈 화면 / 넥스트 세션 조회
```
P-GUEST-OLD     → COHORT_ENDED (latestCohortId != activeCohortId)
P-GUEST-ORG-OLD → COHORT_ENDED
P-DEEPER        → AVAILABLE (세션 있을 때)
P-ORGANIZER     → AVAILABLE (세션 있을 때)
P-MULTI         → 현재 기수 기준 정상 응답
```

---

## 새 API 작성 시 체크리스트

```
[ ] 1. 어떤 @PreAuthorize 권한을 사용하는가?
      → 위 매트릭스에서 각 페르소나 예상 결과 확인

[ ] 2. GUEST(이전 기수 멤버)가 접근할 경우 어떤 응답이 반환되는가?
      → 403이 의도된 동작인가, 아니면 별도 응답이 필요한가?

[ ] 3. 이전 기수 운영진(P-GUEST-ORG-OLD)이 운영진 전용 API에 접근 시 403인가?

[ ] 4. 다기수 멤버(P-MULTI)의 응답이 현재 기수 데이터만 담고 있는가?
      → 쿼리에 cohortId 필터가 활성 기수 기준으로 걸려 있는가?

[ ] 5. 출석 상태 의존 API라면 PENDING(미출석) 케이스를 처리했는가?

[ ] 6. 피드백 관련 API라면 form=null(피드백 미개설) 케이스를 처리했는가?
```

---

## Dev 환경 테스트 계정

계정 정보(이메일·비밀번호·memberId)는 `CLAUDE.local.md` 에 보관한다 (gitignore됨).

---

## 과거에 발견된 버그 패턴

| 증상 | 원인 | 확인 포인트 |
|---|---|---|
| 이전 기수 운영진이 현재 기수 API 접근 성공 | 권한 체크 없이 `isAuthenticated()`만 | `@PreAuthorize` 권한 표현식 확인 |
| P-MULTI 출석 조회에 이전 기수 세션 포함 | cohortId 필터 누락 | 쿼리의 cohortId 조건 확인 |
| GUEST 피드백 API → 403 (NOT_TARGET 아님) | `read:session` 권한 필요 API | 앱 클라이언트 403 처리 여부 확인 |
| 수동 LATE 유저 피드백 푸시 미발송 | scheduler legacy 블록의 `attendedAt == null` 조건 | `SessionFeedbackReminderScheduler` 참고 |
