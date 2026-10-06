# 세션 피드백 API 스펙

> 기준: Figma `코어 3기 - MVP` — 디퍼 > 세션-피드백 / 운영진 > 세션-피드백 (생성) / 운영진 > 세션-피드백 (인사이트) / 운영진 > 세션 (기존)
> 시간 포맷: 기존 API와 동일하게 `LocalDateTime` (KST, `2026-10-08T00:00:00`)
> 응답 래퍼: `CustomResponse<T>` (`status`, `message`, `code`, `data`)
> 아래 예시 JSON은 모두 `data` 안의 내용만 적는다. 실제 응답은 `공통 응답 형식`의 래퍼에 감싸져 내려간다.

---

## 공통 응답 형식 (현재 서버 기준)

`CustomResponse` (`application/.../common/exception/CustomResponse.kt`) 와 로컬 서버 실제 응답(`GET /v1/sessions`, `GET /v1/sessions/next`)으로 확인한 형식이다.

### 성공

```json
{
  "status": "OK",
  "message": "요청에 성공했습니다",
  "code": "GLOBAL-200-01",
  "data": { "...": "..." }
}
```

- `status`는 숫자가 아니라 Spring `HttpStatus` enum 이름 문자열 (`OK`, `BAD_REQUEST`, ...).
- 필드 순서는 `status`, `message`, `code`, `data` 고정 (`@JsonPropertyOrder`).
- **`data`가 null이면 키 자체가 생략된다** (`@JsonInclude(NON_NULL)`). 예: `GET /v1/sessions/next`에서 다음 세션이 없을 때 `{"status":"OK","message":"요청에 성공했습니다","code":"GLOBAL-200-01"}`.
- 단, `data` 내부 DTO의 null 필드는 생략되지 않고 `null`로 내려간다 (전역 Jackson 설정 없음).
- 생성·제출 API도 기존 관례대로 `CustomResponse.ok()` → HTTP 200, `GLOBAL-200-01`, `data` 없음. (`created()`/201은 현재 세션·출석 API에서 쓰지 않음)

### 실패

```json
{
  "status": "BAD_REQUEST",
  "message": "아직 피드백 응답 기간이 아닙니다",
  "code": "SESSION_FEEDBACK-400-04"
}
```

- HTTP 상태 코드와 body의 `status`가 같고, `data`는 없다.
- `BusinessException` 하위 예외 → `GlobalExceptionHandler`가 `XxxExceptionCode`의 status·code·message로 변환.
- 요청 바디 파싱/검증 실패 → `GLOBAL-400-01` (`올바른 입력 형식이 아닙니다.` 또는 `필드명: 메시지`).
- 그 외 예외 → `GLOBAL-500-01` (`예상치 못한 서버 에러가 발생했습니다`).
- 에러 코드 형식: `{DOMAIN}-{HTTP}-{순번 2자리}`. 여러 단어 도메인은 `AFTER_PARTY-`, `BILL_ACCOUNT-`처럼 언더스코어를 쓰므로 `SESSION_FEEDBACK-`으로 맞춘다.

### 값 형식

| 항목 | 형식 | 근거 |
| --- | --- | --- |
| ID | 숫자 (`Long`) | `GET /v1/sessions` → `"id": 35` |
| 일시 | `"2026-03-14T13:00:00"` (오프셋 없음, KST) | 응답 DTO는 `LocalDateTime`, 도메인은 `Instant` (`TimeMapper` 변환) |
| enum | 이름 문자열 (`"IN_PROGRESS"`) | 기존 `AttendanceStatus` 등과 동일 |
| path 변수 | `{sessionId}` → `SessionId` value class 바인딩 | `SessionQueryController` |

---

## 0. API 목록

| # | 구분 | Method | Path | 권한 | 설명 |
| --- | --- | --- | --- | --- | --- |
| 1 | 기존(수정) | `POST` | `/v1/sessions` | `create:session` | 세션 생성 + 피드백 설정 |
| 2 | 기존(수정) | `PATCH` | `/v1/sessions` | `update:session` | 세션 수정 + 피드백 설정 |
| 3 | 기존(수정) | `GET` | `/v1/sessions/{sessionId}` | `create:session` | 운영진 세션 상세 (수정 폼·피드백 탭 상태) |
| 4 | 기존(수정) | `GET` | `/v1/sessions` | `permitAll` → 변경 필요 | 세션 목록 + 세션별 피드백 상태/버튼 노출 여부 |
| 5 | 신규 | `GET` | `/v2/sessions/{sessionId}/feedbacks/me` | `read:session` | 피드백 화면 진입 (세션 정보 + 내 응답 상태 + 문항/후보) |
| 6 | 신규 | `POST` | `/v2/sessions/{sessionId}/feedbacks` | `read:session` | 피드백 제출 |
| 7 | 신규 | `GET` | `/v2/sessions/feedbacks/me/pending` | `read:session` | 홈 카드용 — 지금 작성 가능한 세션 1건 |
| 8 | 신규(예정) | `GET` | `/v2/sessions/{sessionId}/feedbacks/insight` | `update:session` | 운영진 결과(인사이트) 조회 |

> 권한은 신규 리소스 없이 기존 `session` 권한을 재사용하는 안. `SESSION_FEEDBACK` 리소스를 새로 만들 경우 `Resource` enum + seed SQL 동시 추가 필요.

---

## 1. 공통 정책 (Figma 정책 영역 기준)

### 수집 설정
- `피드백 받기` 기본값 **OFF**. OFF여도 세션 생성 가능하며 설문이 생성되지 않음.
- ON이면 **공통 고정 문항**으로 설문 생성. 세션별 문항 수정 미지원.
- `feedbackStartAt`(수집 시작 일시)은 ON일 때 **필수**, 저장 시점 기준 **현재 이후**여야 함.
- `feedbackEndAt` = `feedbackStartAt + 72시간`, 서버에서 계산하며 직접 수정 불가.
- 응답 가능 구간: `feedbackStartAt <= now < feedbackEndAt`. 종료 시각부터 신규 제출 불가.
- `피드백 알림 보내기` 기본값 **ON**. ON이면 수집 시작 시각에 대상자(출석·지각)에게 PUSH 발송. OFF여도 수집은 정상 진행.

### 응답
- 대상: 해당 세션 출결이 **출석(`PRESENT`) 또는 지각(`LATE`)** 인 디퍼.
- 세션당 **1회**, 제출 후 **수정 불가** → DB unique `(session_id, member_id)`, 수정 API 없음.
- 대상 여부·기간·중복 제출은 **서버에서 제출 시점에 다시 검증**.
- 운영진에게 응답자의 이름·팀은 노출하지 않음. 제출 여부는 미응답 알림에만 사용.

### 진입
- 1) 홈 카드, 2) 세션 목록 버튼, 3) 공유 링크 → 모두 같은 피드백 화면 (API #5).
- 홈 카드·세션 목록 버튼은 **응답 기간 중 + 미제출 + 대상자**에게만 노출, 제출하면 사라짐.
- 공유 링크는 로그인 필요 → 로그인 후 해당 설문으로 복귀 (FE 처리). 링크는 FE가 `sessionId`로 생성.
- 앱 진입 시 완료 화면 `닫기`는 진입한 화면으로 복귀, 공유 링크 진입 시 `닫기` 미노출 (FE 처리).

---

## 2. 문항 / 후보 (BE에서 내려줌)

문항은 고정이고, **선택 후보는 BE가 API #5 응답으로 문항별로 내려준다.** FE는 후보를 하드코딩하지 않고 `code`로 제출한다. 인사이트(API #8)의 라벨도 같은 enum에서 내려준다.

### Q1. 만족도 (필수, 단일 선택)
`{sessionTitle} 이번 세션에 얼마나 만족하셨나요? *`

| code | score | 설문 라벨 | 인사이트 라벨 |
| --- | --- | --- | --- |
| `VERY_SATISFIED` | 5 | 매우 만족했어요. | 매우 만족 |
| `SATISFIED` | 4 | 만족했어요. | 만족 |
| `NEUTRAL` | 3 | 보통이었어요. | 보통 |
| `DISSATISFIED` | 2 | 별로였어요. | 불만족 |
| `VERY_DISSATISFIED` | 1 | 매우 별로였어요. | 매우 불만족 |

### Q2. 좋았던 점 (필수, 최대 2개)
`이번 세션에서 특히 좋았던 부분이 있었나요? *` / `최대 2개까지 선택해주세요.`

| code | 라벨 |
| --- | --- |
| `SESSION_CONTENT` | 세션 내용 |
| `PROGRESS_AND_TIME` | 진행 방식·시간 |
| `NETWORKING` | 교류 기회 |
| `GUIDANCE` | 사전·현장 안내 |
| `PLACE_AND_ACCESS` | 장소·접속 환경 |
| `ETC` | 기타 |
| `NOTHING` | 특별히 없음 |

### Q3. 개선이 필요한 점 (필수, 최대 2개)
`이번 세션에서 개선이 필요한 부분이 있었나요? *` / `최대 2개까지 선택해주세요.`

| code | 라벨 |
| --- | --- |
| `SESSION_CONTENT` | 세션 내용 |
| `PROGRESS_AND_TIME` | 진행 방식·시간 |
| `NETWORKING` | 교류 기회 |
| `GUIDANCE` | 사전·현장 안내 |
| `PLACE_AND_ACCESS` | 장소·접속 환경 |
| `ETC` | 기타 |
| `NOTHING` | 특별히 없음 |

- 현재 Q2·Q3 후보는 동일하지만 문항별로 따로 내려주므로, 이후 한쪽만 바뀌어도 FE 수정 없이 반영 가능.
- `ETC` 선택 시 `기타 의견 *` 텍스트 **필수** (placeholder: `어떤 부분의 개선이 필요한지 적어주세요.`).
- `NOTHING`은 다른 후보와 함께 선택 불가 (확인 필요).
- 라벨 구분점은 Figma에 `·`/`∙`가 섞여 있음 → BE에서 `·`로 통일.

### Q4. 자유 의견 (선택)
`세션에 대해 더 전하고 싶은 이야기가 있나요?`
placeholder: `앞에서 고른 항목의 이유나 그 밖의 의견을 자유롭게 남겨주세요. (선택)`

---

## 3. 세션 생성 / 수정 (기존 수정) — #1, #2

기존 `SessionCreateRequest` / `SessionUpdateRequest`에 필드 추가.

```json
{
  "name": "디프만 19기 OT",
  "date": "2026-10-04T14:00:00",
  "isOnline": false,
  "place": "공덕 창업허브",
  "week": 1,
  "attendanceStart": "2026-10-04T14:00:00",
  "lateStart": "2026-10-04T14:16:00",
  "absentStart": "2026-10-04T14:31:00",

  "feedbackEnabled": true,
  "feedbackStartAt": "2026-10-04T18:00:00",
  "feedbackPushEnabled": true
}
```

| 필드 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `feedbackEnabled` | Boolean | N | `false` | 피드백 받기 |
| `feedbackStartAt` | LocalDateTime | `feedbackEnabled=true`면 Y | - | 피드백 시작 시간, 현재 이후 |
| `feedbackPushEnabled` | Boolean | N | `true` | 피드백 알림 보내기 |

검증 실패 시 (상세는 `11. 에러 처리`):
- `feedbackEnabled=true`인데 `feedbackStartAt` 없음 → `SESSION_FEEDBACK-400-01`
- `feedbackStartAt <= now` → `SESSION_FEEDBACK-400-02`
- 수정 시 이미 수집이 시작된 세션의 `feedbackEnabled`/`feedbackStartAt` 변경 → `SESSION_FEEDBACK-409-02` (안, 확인 필요 4번)
- `feedbackEnabled=false`면 `feedbackStartAt`/`feedbackPushEnabled`는 무시 (에러 아님)

> 저장 성공 시점에 피드백 설정 적용. PUSH는 `feedbackStartAt`에 스케줄러가 발송 (`NotificationMessageType.SESSION_FEEDBACK_OPENED` 신규).

---

## 4. 운영진 세션 상세 (기존 수정) — #3

`SessionDetailResponse`에 `feedback` 추가. 수정 폼 프리필과 상세 패널의 `피드백` 탭 헤더에 사용.

```json
{
  "id": 1,
  "week": 1,
  "name": "디프만 19기 OT",
  "...": "기존 필드 유지",
  "feedback": {
    "status": "IN_PROGRESS",
    "startAt": "2026-10-08T00:00:00",
    "endAt": "2026-10-11T00:00:00",
    "pushEnabled": true
  }
}
```

- 피드백 받기 OFF면 `feedback: null`.
- 탭 헤더 문구: `피드백 수집 시작 전 / 시작 중 / 마감 ∙ {startAt} ~ {endAt}`.
- 피드백 링크 복사는 FE가 `sessionId`로 생성.

### 피드백 상태 `SessionFeedbackStatus`

| 값 | 조건 | 운영진 목록 배지 |
| --- | --- | --- |
| `SCHEDULED` | `now < startAt` | 피드백 수집 예정 |
| `IN_PROGRESS` | `startAt <= now < endAt` | 피드백 수집 중 |
| `CLOSED` | `endAt <= now` (응답 0건이어도) | 피드백 결과 보기 |
| (`feedback = null`) | 피드백 받기 OFF | 배지 미노출 |

---

## 5. 세션 목록 (기존 수정) — #4

`SessionListDetailResponse`에 `feedback` 추가. 운영진 목록 배지와 디퍼 세션 목록의 `피드백 남기기 (D-n)` 버튼에 같이 사용.

```json
{
  "sessions": [
    {
      "id": 1,
      "week": 1,
      "name": "디프만 19기 OT",
      "date": "2026-10-04T14:00:00",
      "place": "공덕 창업허브",
      "isOnline": false,
      "feedback": {
        "status": "IN_PROGRESS",
        "endAt": "2026-10-11T00:00:00",
        "canSubmit": true
      }
    }
  ]
}
```

- `canSubmit` = 로그인한 멤버 기준 `IN_PROGRESS` + 대상자 + 미제출. 디퍼 버튼 노출 여부.
- `D-n`은 FE가 `endAt`으로 계산.
- 현재 `permitAll`이라 멤버 식별 불가 → **로그인 필수로 변경**하거나, 비로그인 시 `canSubmit=false`로 내려주는 방식 중 결정 필요.

---

## 6. 피드백 화면 진입 (신규) — #5

`GET /v2/sessions/{sessionId}/feedbacks/me`

홈 카드·세션 목록·공유 링크 공통. **상태 화면을 위해 에러가 아니라 200 + `myStatus`로 응답**한다.

```json
{
  "sessionId": 1,
  "week": 1,
  "sessionName": "디프만 19기 OT",
  "startAt": "2026-10-08T00:00:00",
  "endAt": "2026-10-11T00:00:00",
  "myStatus": "AVAILABLE",
  "questions": {
    "satisfaction": {
      "title": "디프만 19기 OT\n이번 세션에 얼마나 만족하셨나요?",
      "required": true,
      "options": [
        { "code": "VERY_SATISFIED", "score": 5, "label": "매우 만족했어요." },
        { "code": "SATISFIED", "score": 4, "label": "만족했어요." },
        { "code": "NEUTRAL", "score": 3, "label": "보통이었어요." },
        { "code": "DISSATISFIED", "score": 2, "label": "별로였어요." },
        { "code": "VERY_DISSATISFIED", "score": 1, "label": "매우 별로였어요." }
      ]
    },
    "likedAspects": {
      "title": "이번 세션에서 특히 좋았던 부분이 있었나요?",
      "description": "최대 2개까지 선택해주세요.",
      "required": true,
      "maxSelect": 2,
      "etcPlaceholder": "어떤 부분이 좋았는지 적어주세요.",
      "options": [
        { "code": "SESSION_CONTENT", "label": "세션 내용" },
        { "code": "PROGRESS_AND_TIME", "label": "진행 방식·시간" },
        { "code": "NETWORKING", "label": "교류 기회" },
        { "code": "GUIDANCE", "label": "사전·현장 안내" },
        { "code": "PLACE_AND_ACCESS", "label": "장소·접속 환경" },
        { "code": "ETC", "label": "기타", "requiresText": true },
        { "code": "NOTHING", "label": "특별히 없음", "exclusive": true }
      ]
    },
    "improvementAspects": {
      "title": "이번 세션에서 개선이 필요한 부분이 있었나요?",
      "description": "최대 2개까지 선택해주세요.",
      "required": true,
      "maxSelect": 2,
      "etcPlaceholder": "어떤 부분의 개선이 필요한지 적어주세요.",
      "options": [
        { "code": "SESSION_CONTENT", "label": "세션 내용" },
        { "code": "PROGRESS_AND_TIME", "label": "진행 방식·시간" },
        { "code": "NETWORKING", "label": "교류 기회" },
        { "code": "GUIDANCE", "label": "사전·현장 안내" },
        { "code": "PLACE_AND_ACCESS", "label": "장소·접속 환경" },
        { "code": "ETC", "label": "기타", "requiresText": true },
        { "code": "NOTHING", "label": "특별히 없음", "exclusive": true }
      ]
    },
    "freeComment": {
      "title": "세션에 대해 더 전하고 싶은 이야기가 있나요?",
      "placeholder": "앞에서 고른 항목의 이유나 그 밖의 의견을 자유롭게 남겨주세요. (선택)",
      "required": false
    }
  }
}
```

### `myStatus` ↔ 화면

| 값 | 화면 문구 |
| --- | --- |
| `AVAILABLE` | 설문 작성 |
| `BEFORE_START` | 아직 피드백 응답 기간이 아니에요. / `{startAt}`부터 작성할 수 있어요. |
| `SUBMITTED` | 이미 피드백을 제출했어요. |
| `CLOSED` | 피드백 응답 기간이 끝났어요. |
| `NOT_TARGET` | 이 세션의 피드백 대상이 아니에요. |
| `DISABLED` | 피드백 받기 OFF 세션 (Figma 화면 없음 → `NOT_TARGET` 화면 재사용 여부 확인) |

- 판정 우선순위: `DISABLED` → `NOT_TARGET` → `SUBMITTED` → `BEFORE_START` → `CLOSED` → `AVAILABLE`
- `questions`는 `AVAILABLE`일 때만 내려주고, 나머지 상태에선 `null`.
- 상태 화면은 에러가 아니므로 이 API의 에러는 인증(401)·세션 없음(404)·서버 오류(500)뿐이다 (`11-4`).

---

## 7. 피드백 제출 (신규) — #6

`POST /v2/sessions/{sessionId}/feedbacks`

```json
{
  "satisfaction": "VERY_SATISFIED",
  "likedAspects": ["SESSION_CONTENT", "ETC"],
  "likedEtc": "현직자 질의응답이 좋았어요.",
  "improvementAspects": ["PROGRESS_AND_TIME"],
  "improvementEtc": null,
  "freeComment": "질문 시간이 조금 더 길었으면 좋겠어요."
}
```

응답: `CustomResponse<Void>` (HTTP 200, `GLOBAL-200-01`, `data` 없음 — 기존 `POST /v2/sessions/{sessionId}/absence-reasons`와 동일) → 완료 화면 `피드백을 제출했어요. / 소중한 의견 감사해요. 다음 세션을 준비할 때 참고할게요!`

### 검증 (진입 API와 같은 판정을 제출 시점에 재수행)

검증 순서와 에러 코드는 `11-4`의 #6 피드백 제출에 정리한다.

- `ETC`를 고르지 않았는데 `likedEtc`/`improvementEtc`가 오면 에러 없이 무시하고 `null`로 저장.
- `freeComment`의 빈 문자열·공백만 있는 값은 `null`로 저장.
- 작성 중 나가기(`피드백을 그만둘까요? / 작성중인 내용이 사라져요.`)는 FE 전용, API 없음.

---

## 8. 홈 카드용 조회 (신규) — #7

`GET /v2/sessions/feedbacks/me/pending`

지금 작성 가능한 세션 1건(`canSubmit=true`)을 반환한다. 없으면 `GET /v1/sessions/next`와 같이 **`data` 키가 생략된** 200 응답을 내려준다 (FE는 `data` 없음 = 카드 미노출).

```json
{
  "sessionId": 1,
  "week": 1,
  "sessionName": "디프만 19기 OT",
  "endAt": "2026-10-11T00:00:00"
}
```

- 대상이 여러 건이면 **`endAt`이 가장 빠른 세션** 1건.
- 카드 문구: `{week}주차 세션 / {sessionName} / 좋았던 점이나 아쉬웠던 점을 알려주세요. / 피드백 남기기 (D-n)`

---

## 9. 운영진 결과(인사이트) 조회 (신규 예정) — #8

`GET /v2/sessions/{sessionId}/feedbacks/insight`

- 마감 후 한 번에 공개하지 않고 **제출 즉시 실시간 누적**. 수집 중에도 조회 가능.
- 응답자 식별 정보(memberId·이름·팀)는 포함하지 않음.

```json
{
  "sessionId": 1,
  "sessionName": "디프만 19기 OT",
  "status": "CLOSED",
  "startAt": "2026-10-08T00:00:00",
  "endAt": "2026-10-11T00:00:00",
  "responseSummary": {
    "respondentCount": 24,
    "targetCount": 40,
    "responseRate": 60
  },
  "satisfaction": {
    "average": 4.2,
    "maxScore": 5,
    "distribution": [
      { "code": "VERY_SATISFIED", "label": "매우 만족", "count": 11, "rate": 46 },
      { "code": "SATISFIED", "label": "만족", "count": 8, "rate": 33 },
      { "code": "NEUTRAL", "label": "보통", "count": 4, "rate": 17 },
      { "code": "DISSATISFIED", "label": "불만족", "count": 1, "rate": 4 },
      { "code": "VERY_DISSATISFIED", "label": "매우 불만족", "count": 0, "rate": 0 }
    ]
  },
  "likedAspects": {
    "totalSelectionCount": 50,
    "items": [
      { "code": "SESSION_CONTENT", "label": "세션 내용", "count": 32, "rate": 64 }
    ],
    "etcComments": ["현직자 질의응답이 좋았어요."]
  },
  "improvementAspects": {
    "totalSelectionCount": 50,
    "items": [
      { "code": "PROGRESS_AND_TIME", "label": "진행 방식·시간", "count": 14, "rate": 28 }
    ],
    "etcComments": [
      "질문 시간이 조금 더 길었으면 좋겠어요.",
      "세션 자료를 미리 공유해주면 좋겠어요."
    ]
  },
  "freeComments": {
    "count": 9,
    "items": ["활동 방향을 이해하는 데 도움이 됐어요. ..."]
  }
}
```

### 계산 규칙

| 항목 | 규칙 |
| --- | --- |
| `responseRate` | `respondentCount / targetCount` (대상 = 조회 시점의 출석·지각자 수), 정수 % |
| `satisfaction.average` | 소수 첫째 자리 |
| `distribution` | 5 → 1점 순서 고정, 0건도 포함. `rate` = count / 응답자 수 |
| `items` | 응답 수 내림차순 정렬 (BE). `rate` = 해당 항목 선택 수 / 문항 응답자 수 (복수 선택) |
| `totalSelectionCount` | 선택 총건수 (`총 50건 ∙ 복수 선택`) |
| `ETC` | 선택 0건이면 `items`에서 제외, `etcComments` 빈 배열 |
| `freeComments` | 빈 문자열 제외, 제출 순 정렬 |

FE 처리: 만족도 최다 항목 강조(동률 모두), 선택형 상위 3개 강조, 기타 아코디언(기본 접힘), 자유 의견 5줄 말줄임·더보기.

### 상태별 화면

| 조건 | 화면 |
| --- | --- |
| `SCHEDULED` | 피드백 수집 예정이에요 (수집 시작 전) |
| `IN_PROGRESS` + 응답 0건 | 아직 받은 피드백이 없어요 (수집 중 · 응답 0건) |
| `CLOSED` + 응답 0건 | 수집된 피드백이 없어요 (수집 마감 · 응답 0건) |
| 조회 실패 (5xx) | 피드백을 불러오지 못했어요 (결과 불러오기 실패) |
| 피드백 OFF 세션 | `SESSION_FEEDBACK-400-03` |

---

## 10. 데이터 모델 (안)

### `session_feedback_forms` (세션 1:1, 피드백 받기 ON일 때만 생성)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `session_feedback_form_id` | BIGINT PK | |
| `session_id` | BIGINT UNIQUE | `sessions` FK |
| `start_at` | DATETIME | 수집 시작 |
| `end_at` | DATETIME | `start_at + 72h` |
| `push_enabled` | BOOLEAN | 피드백 알림 보내기 |
| `push_sent_at` | DATETIME NULL | 중복 발송 방지 |
| `created_at` / `updated_at` / `deleted_at` | DATETIME | |

### `session_feedbacks` (응답)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `session_feedback_id` | BIGINT PK | |
| `session_id` | BIGINT | |
| `member_id` | BIGINT | 제출 여부·중복 판정 전용, 인사이트 응답엔 미노출 |
| `satisfaction` | VARCHAR | `SessionFeedbackSatisfaction` |
| `liked_etc` | VARCHAR NULL | |
| `improvement_etc` | VARCHAR NULL | |
| `free_comment` | TEXT NULL | |
| `submitted_at` | DATETIME | |
| UNIQUE | `(session_id, member_id)` | 세션당 1회 |

### `session_feedback_aspects` (선택 항목, 집계용)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `session_feedback_aspect_id` | BIGINT PK | |
| `session_feedback_id` | BIGINT | |
| `question` | VARCHAR | `LIKED` / `IMPROVEMENT` |
| `aspect` | VARCHAR | `SessionFeedbackAspect` |

---

## 10-1. DB 관리 방식 (현재 서버 기준)

Flyway/Liquibase 같은 자동 마이그레이션 도구는 없다. **JPA 엔티티 + 수동 SQL 스크립트**로 관리한다.

### 스키마의 기준

| 항목 | 내용 |
| --- | --- |
| 스키마 기준(source of truth) | `entity` 모듈의 JPA 엔티티 (`core.entity.*`) |
| dev / prod | `ddl-auto: validate` (`application.yml`) — 엔티티와 실제 테이블이 다르면 **서버 기동 실패** |
| local | `ddl-auto: update` (`application-local.yml`) — 엔티티 기준으로 로컬 DB에 테이블·컬럼 자동 생성 |
| jOOQ DSL | `codegen` 모듈이 **DB가 아니라 JPA 엔티티**(`JPADatabase`, `packages=core.entity`)를 읽어 생성. 컴파일 시 자동 생성 (`generateSchemaSourceOnCompilation`), 수동은 `./gradlew :codegen:jooqGenerate` |
| DB | MySQL 8, 같은 RDS 인스턴스에서 스키마로 dev/prod 분리 |

> `src/main/resources/db/schema.sql`은 구버전 모듈 것이라 현재 스키마 기준으로 쓰지 않는다.

### 반영 절차

1. **엔티티 추가** — `entity/src/main/kotlin/core/entity/sessionFeedback/`에 `SessionFeedbackFormEntity`, `SessionFeedbackEntity`, `SessionFeedbackAspectEntity` 작성. unique 제약(`(session_id, member_id)`)도 엔티티에 선언.
2. **로컬 확인** — local 프로필(`ddl-auto: update`)로 기동해 테이블이 생성되는지 확인. jOOQ DSL(`SESSION_FEEDBACKS` 등)은 빌드 시 자동 생성.
3. **SQL 작성** — `db/pending/YYMMDDHHMM_session_feedback.sql` (예: `2610062130_session_feedback.sql`).
4. **dev/prod 적용** — 배포 **전에** SQL을 먼저 실행. `validate` 때문에 테이블이 없으면 새 서버가 뜨지 않는다.
5. **정리** — 적용이 끝난 스크립트는 `db/release/{YYMMDDHHMM}/`로 옮긴다.

### SQL 작성 규칙 (기존 `db/pending/*.sql` 형식)

- 상단 헤더 주석: 날짜·제목 / 목적 / 선행 / 후행 / 검증 / 주의(`ddl-auto: validate`라 배포 전 적용 필요).
- `[1]`, `[2]` 단위로 구역을 나누고 `START TRANSACTION; ... COMMIT;`으로 감싼다.
- **재실행해도 안전하게** 작성: `information_schema`로 존재 여부를 확인한 뒤 `PREPARE`/`EXECUTE`로 실행하거나 `CREATE TABLE IF NOT EXISTS` 사용.
- 하단에 읽기 전용 `VERIFY` 쿼리를 둔다.
- 컬럼 타입·이름은 엔티티와 정확히 일치해야 한다 (`validate` 통과 조건).

```sql
-- =============================================================================
-- 2026-10-06 · 세션 피드백 테이블 추가
-- =============================================================================
-- 목적: 세션 피드백 설정·응답·선택 항목 저장
--       1) session_feedback_forms  2) session_feedbacks  3) session_feedback_aspects
-- 선행: 없음
-- 후행: 없음
-- 검증: 파일 하단 VERIFY 섹션 (읽기 전용)
-- 주의: ddl-auto: validate 이므로 이 스크립트를 먼저 적용한 뒤 배포해야 한다.

-- -----------------------------------------------------------------------------
-- [1] session_feedback_forms
-- -----------------------------------------------------------------------------
START TRANSACTION;
CREATE TABLE IF NOT EXISTS session_feedback_forms ( ... );
COMMIT;

-- ... [2], [3] 동일

-- -----------------------------------------------------------------------------
-- VERIFY
-- -----------------------------------------------------------------------------
-- SHOW CREATE TABLE session_feedbacks;
```

> 피드백 정보는 `sessions` 테이블에 컬럼을 추가하지 않고 별도 테이블로 둔다. 기존 `sessions`/`SessionEntity`를 건드리지 않아 `validate` 영향 범위가 신규 테이블로 한정된다.

---

## 11. 에러 처리

### 11-1. 원칙

- **상태 화면은 에러가 아니다.** 진입(#5)·목록(#4)·홈 카드(#7)는 "기간 아님/제출함/대상 아님"을 200 + 상태값(`myStatus`, `canSubmit`, `data` 생략)으로 내려준다. 에러는 **제출(#6)·설정 저장(#1, #2)** 처럼 요청 자체를 거절해야 할 때만 쓴다.
- 비즈니스 에러는 `BusinessException` 하위 클래스 + `SessionFeedbackExceptionCode` enum으로 정의하고, `GlobalExceptionHandler`가 HTTP 상태·`code`·`message`로 변환한다 (기존 `AttendanceExceptionCode`/`AbsenceReasonRequiredException`과 같은 구조).
  - 위치: `application/.../sessionFeedback/application/exception/`
- 요청 형식 오류(필수값 누락, 잘못된 enum, 길이 초과)는 Jackson·Bean Validation 단계에서 `GLOBAL-400-01`로 끝난다. 서비스 로직의 비즈니스 검증은 그 뒤에 실행된다.
- FE가 화면을 바꿔야 하는 에러는 코드를 따로 두고, FE 검증을 통과했다면 나올 수 없는 에러(응답값 규칙 위반)는 원인 추적용으로 코드를 나누되 FE에서는 한 가지로 처리해도 된다.

### 11-2. 공통 에러 (현재 서버 동작, 로컬에서 직접 호출해 확인)

`/v1/**`, `/v2/**`는 `SecurityConfig`에서 전부 `permitAll`이라 필터 단계에서는 막히지 않는다. 인증·권한은 컨트롤러 단계에서 검사된다.

| 상황 | HTTP | code | message | 발생 위치 |
| --- | --- | --- | --- | --- |
| 토큰 없음 + `@CurrentMemberId` 사용 API (#5, #6, #7) | 401 | `GLOBAL-401-01` | 로그인이 필요합니다. | `CurrentMemberIdArgumentResolver` |
| 토큰 없음 + `@PreAuthorize`만 있는 API (#3, #8) | 403 | `GLOBAL-403-01` | 요청 권한이 없습니다. | `AuthorizationDeniedException` |
| 권한 없음 (디퍼가 #1·#2·#3·#8 호출) | 403 | `GLOBAL-403-01` | 요청 권한이 없습니다. | 〃 |
| 요청 바디 필수값 누락 | 400 | `GLOBAL-400-01` | `satisfaction: 필드 값이 누락되었거나 타입이 맞지 않습니다` | `HttpMessageNotReadableException` |
| 존재하지 않는 enum 값 (`"satisfaction": "GOOD"`) | 400 | `GLOBAL-400-01` | `satisfaction: 올바른 형식이어야 합니다` | 〃 |
| 길이 초과 (`@field:Size`) | 400 | `GLOBAL-400-01` | `freeComment: {Size 메시지}` | `MethodArgumentNotValidException` |
| 세션 없음·삭제됨 | 404 | `SESSION-404-01` | 세션을 찾을 수 없습니다 | `SessionNotFoundException` |
| 그 외 예외 | 500 | `GLOBAL-500-01` | 예상치 못한 서버 에러가 발생했습니다 | `handleException` |

> **기존 서버의 한계 (이번 기능 범위 밖, 별도 수정 권장)**
> - **잘못되거나 만료된 토큰**: `JwtAuthenticationFilter`가 필터 안에서 `InvalidAccessTokenException`을 던지는데, `@RestControllerAdvice`까지 가지 못한다. 그래서 `CustomResponse`가 아닌 Spring 기본 응답(`{"timestamp":...,"status":500,"error":"Internal Server Error","path":...}`, HTTP 500)이 나간다. 모든 API에 공통인 동작이다.
> - **path 변수 타입 오류** (`/v2/sessions/abc/feedbacks/me`): `MethodArgumentTypeMismatchException` 핸들러가 없어 `GLOBAL-500-01`(500)로 나간다. 핸들러를 추가해 `GLOBAL-400-01`로 바꾸는 것을 권장한다.

### 11-3. `SessionFeedbackExceptionCode` (신규)

| enum | HTTP | code | message | 사용 API |
| --- | --- | --- | --- | --- |
| `FEEDBACK_START_AT_REQUIRED` | 400 | `SESSION_FEEDBACK-400-01` | 피드백 시작 시간을 입력해주세요 | #1, #2 |
| `INVALID_FEEDBACK_START_AT` | 400 | `SESSION_FEEDBACK-400-02` | 피드백 시작 시간은 현재 이후여야 합니다 | #1, #2 |
| `FEEDBACK_DISABLED` | 400 | `SESSION_FEEDBACK-400-03` | 피드백을 받지 않는 세션입니다 | #6, #8 |
| `FEEDBACK_NOT_STARTED` | 400 | `SESSION_FEEDBACK-400-04` | 아직 피드백 응답 기간이 아닙니다 | #6 |
| `FEEDBACK_CLOSED` | 400 | `SESSION_FEEDBACK-400-05` | 피드백 응답 기간이 끝났습니다 | #6 |
| `INVALID_ASPECT_COUNT` | 400 | `SESSION_FEEDBACK-400-06` | 항목은 1개 이상 2개 이하로 선택해주세요 | #6 |
| `DUPLICATED_ASPECT` | 400 | `SESSION_FEEDBACK-400-07` | 같은 항목을 중복으로 선택할 수 없습니다 | #6 |
| `EXCLUSIVE_ASPECT_COMBINED` | 400 | `SESSION_FEEDBACK-400-08` | '특별히 없음'은 다른 항목과 함께 선택할 수 없습니다 | #6 |
| `ETC_TEXT_REQUIRED` | 400 | `SESSION_FEEDBACK-400-09` | 기타 의견을 입력해주세요 | #6 |
| `NOT_FEEDBACK_TARGET` | 403 | `SESSION_FEEDBACK-403-01` | 피드백 대상이 아닙니다 | #6 |
| `ALREADY_SUBMITTED_FEEDBACK` | 409 | `SESSION_FEEDBACK-409-01` | 이미 피드백을 제출했습니다 | #6 |
| `FEEDBACK_ALREADY_STARTED` | 409 | `SESSION_FEEDBACK-409-02` | 피드백 수집이 시작되어 설정을 변경할 수 없습니다 | #2 (안) |

- 기간 관련 에러를 400으로 둔 것은 기존 `TOO_EARLY_ATTENDANCE`(`SESSION-400-03`)와 맞춘 것이다.
- 중복 제출을 409로 둔 것은 기존 `ATTENDANCE-409-01`과 맞춘 것이다. (`ALREADY_CHECKED_ATTENDANCE`는 400이지만 새 코드는 의미가 분명한 409를 쓴다.)
- `NOT_FEEDBACK_TARGET`(403)은 권한 에러 `GLOBAL-403-01`과 같은 HTTP 상태지만, 코드로 구분된다.

### 11-4. API별 에러

`GLOBAL-500-01`과 11-2의 토큰 관련 동작은 모든 API에 공통이라 생략한다.

**#1, #2 세션 생성·수정** (기존 에러 + 아래 추가)

| 순서 | 조건 | code |
| --- | --- | --- |
| 1 | 권한 없음 | `GLOBAL-403-01` |
| 2 | 바디 형식 오류 | `GLOBAL-400-01` |
| 3 | (수정) 세션 없음 | `SESSION-404-01` |
| 4 | `feedbackEnabled=true`인데 `feedbackStartAt` 없음 | `SESSION_FEEDBACK-400-01` |
| 5 | `feedbackStartAt <= now` | `SESSION_FEEDBACK-400-02` |
| 6 | (수정) 이미 `startAt <= now`인 세션의 `feedbackEnabled`/`feedbackStartAt` 변경 | `SESSION_FEEDBACK-409-02` (안) |

- 수정 시 `feedbackStartAt`이 **기존 값과 같으면** 5·6번 검증을 하지 않는다. 그렇지 않으면 수집이 시작된 세션은 세션 이름만 고쳐도 400-02가 나기 때문이다.
- `feedbackPushEnabled` 변경은 언제나 허용한다. 이미 발송된 뒤라면 효과가 없다.

**#3 운영진 세션 상세** — 기존과 동일 (`GLOBAL-403-01`, `SESSION-404-01`). 피드백 OFF는 에러가 아니라 `feedback: null`.

**#4 세션 목록** — 기존과 동일. 비로그인 처리 방식은 확인 필요 3번에 따른다 (로그인 필수로 바꾸면 `GLOBAL-401-01` 추가).

**#5 피드백 화면 진입**

| 조건 | 응답 |
| --- | --- |
| 비로그인 | 401 `GLOBAL-401-01` → FE가 로그인 후 원래 링크로 복귀 |
| 세션 없음·삭제됨 | 404 `SESSION-404-01` |
| 피드백 OFF·대상 아님·제출함·기간 아님 | **200** + `myStatus` (에러 아님) |

**#6 피드백 제출** — 위에서부터 순서대로 검사하고 처음 걸린 에러를 반환한다. 진입 API의 `myStatus` 판정 순서와 같게 맞춰서, 같은 시점이면 화면과 에러가 어긋나지 않게 한다.

| 순서 | 조건 | code |
| --- | --- | --- |
| 1 | 비로그인 | `GLOBAL-401-01` |
| 2 | 바디 형식 오류 (필수값 누락, 없는 enum, 길이 초과) | `GLOBAL-400-01` |
| 3 | 세션 없음·삭제됨 | `SESSION-404-01` |
| 4 | 피드백 OFF 세션 | `SESSION_FEEDBACK-400-03` |
| 5 | 출석·지각자 아님 (다른 기수, 결석, 출결 없음 포함) | `SESSION_FEEDBACK-403-01` |
| 6 | 이미 제출 | `SESSION_FEEDBACK-409-01` |
| 7 | `now < startAt` | `SESSION_FEEDBACK-400-04` |
| 8 | `endAt <= now` | `SESSION_FEEDBACK-400-05` |
| 9 | 좋았던 점·개선점 선택 수가 0개 또는 3개 이상 | `SESSION_FEEDBACK-400-06` |
| 10 | 한 문항 안에서 같은 코드 중복 | `SESSION_FEEDBACK-400-07` |
| 11 | `NOTHING` + 다른 항목 동시 선택 | `SESSION_FEEDBACK-400-08` (확인 필요 2번에 따라 제거 가능) |
| 12 | `ETC` 선택했는데 기타 텍스트가 없거나 공백만 있음 | `SESSION_FEEDBACK-400-09` |

- 기간 판정 기준 시각은 **서버가 요청을 받은 시각**이다. 작성 중 마감 시각이 지나면 400-05가 나간다.
- 동시 요청으로 unique `(session_id, member_id)` 위반(`DataIntegrityViolationException`)이 나면 500이 아니라 `SESSION_FEEDBACK-409-01`로 변환한다. 저장은 `saveAndFlush`로 해서 서비스 안에서 예외를 잡을 수 있게 한다.
- 텍스트 길이 제한은 Bean Validation(`@field:Size`)으로 걸어 `GLOBAL-400-01`로 처리한다. 최대 길이는 확인 필요 6번 (안: 기타 100자, 자유 의견 1,000자).

**#7 홈 카드** — 비로그인 `GLOBAL-401-01`만 있다. 작성할 세션이 없으면 에러가 아니라 `data` 생략 200.

**#8 인사이트**

| 조건 | code |
| --- | --- |
| 비로그인·권한 없음 | `GLOBAL-403-01` |
| 세션 없음·삭제됨 | `SESSION-404-01` |
| 피드백 OFF 세션 | `SESSION_FEEDBACK-400-03` |

- 수집 전·응답 0건은 에러가 아니라 200 + `status`/`respondentCount=0`으로 내려주고, FE가 빈 상태 화면을 보여준다.

### 11-5. FE 처리 매핑 (Figma 화면 기준)

| 응답 | FE 화면 |
| --- | --- |
| `SESSION_FEEDBACK-400-04` | 아직 피드백 응답 기간이 아니에요. (`BEFORE_START` 화면) |
| `SESSION_FEEDBACK-400-05` | 피드백 응답 기간이 끝났어요. (`CLOSED` 화면) |
| `SESSION_FEEDBACK-403-01` | 이 세션의 피드백 대상이 아니에요. (`NOT_TARGET` 화면) |
| `SESSION_FEEDBACK-409-01` | 이미 피드백을 제출했어요. (`SUBMITTED` 화면) |
| `SESSION_FEEDBACK-400-03` | `NOT_TARGET` 화면 재사용 (확인 필요) |
| `SESSION_FEEDBACK-400-06` ~ `400-09`, `GLOBAL-400-01` | FE 검증 누락 상황 → 제출 실패 모달 + `message` 로깅 |
| `GLOBAL-401-01` | 로그인 화면 → 로그인 후 같은 피드백 링크로 복귀 |
| `SESSION-404-01` | 세션을 찾을 수 없음 (Figma 화면 없음, 확인 필요) |
| 5xx·네트워크 오류 | `피드백을 제출하지 못했어요. / 잠시 후 다시 시도해 주세요.` + `다시 시도` |
| #8 5xx | 피드백을 불러오지 못했어요. |

- 제출 중 화면이 바뀌는 에러(400-04/05, 403-01, 409-01)는 작성 내용을 버리고 해당 상태 화면으로 바꾼다. `다시 시도` 모달은 5xx·네트워크 오류일 때만 띄운다.

---

## 12. 확인 필요

1. **수집 시작 기준**: 디퍼 정책은 "세션 종료 후부터 3일간", 생성 정책은 "직접 지정한 시작 시각부터 72시간". 이 문서는 생성 정책 기준.
2. **`특별히 없음`과 다른 항목 동시 선택** 허용 여부.
3. **세션 목록 `permitAll`**: 로그인 필수로 바꿀지, 비로그인 시 `canSubmit=false`로 둘지.
4. **수정 제약**: 수집 시작 후 시작 시각 변경, 응답이 있는데 피드백 OFF로 변경할 때 허용 여부 (Figma 미정의). 이 문서는 수집 시작 후 `feedbackEnabled`/`feedbackStartAt` 변경을 `SESSION_FEEDBACK-409-02`로 막는 안으로 작성.
5. **대상 판정 시점**: 출결이 나중에 수정되면 대상/응답률도 같이 바뀜 (조회 시점 기준으로 가정).
6. **텍스트 길이 제한**: 기타·자유 의견 최대 글자 수 (Figma 미정의).
7. **미응답 알림**: "제출 여부는 미응답 알림에만 활용" 문구가 있으나 발송 시점·API 없음 → 이번 범위 제외로 가정.
8. **권한**: 기존 `session` 권한 재사용 vs `SESSION_FEEDBACK` 리소스 신규.
9. **에러 화면**: 피드백 OFF 세션(`400-03`)과 세션 없음(`SESSION-404-01`)에 해당하는 Figma 화면이 없음 → `NOT_TARGET` 화면 재사용 여부.
10. **기존 공통 에러 개선 여부**: 만료·잘못된 토큰이 Spring 기본 500으로 나가는 문제, path 타입 오류가 500으로 나가는 문제(`11-2`)를 이번에 같이 고칠지.

---

## 13. 이슈 / 브랜치 / PR 작업 방식

API·기능 단위로 GitHub Issue를 나누고, **이슈 번호로 브랜치를 따서** 이슈당 PR 1개를 만든다.  
레포 관례: `feat/{domain}/#{이슈번호}-{slug}` → PR → `Closes #{이슈번호}` (예: `feat/member/#565-token-login-method`).

### 13-1. 작업 흐름

1. **이슈 생성** — 아래 표 순서대로 GitHub Issue 생성 (라벨 `✨ Feature`).
2. **브랜치** — 기준 브랜치(`develop` 또는 선행 PR 브랜치)에서  
   `git checkout -b feat/session-feedback/#{이슈번호}-{slug}`
3. **구현** — 해당 이슈 Tasks만. 스펙은 이 문서 + 이슈 body.
4. **PR** — base는 선행 이슈가 머지된 브랜치(또는 `develop`). body에 `Closes #{이슈번호}`.
5. **머지 후** — 다음 이슈 브랜치는 방금 머지된 기준에서 다시 딴다 (스택 PR이면 선행 head 위에 쌓아도 됨).

### 13-2. 이슈 분할 (권장 순서)

| 순서 | 이슈 | 제목 | 스펙 | 브랜치 | 선행 |
| --- | --- | --- | --- | --- | --- |
| A | [#595](https://github.com/depromeet/dpm-core-server/issues/595) | 도메인·엔티티·마이그레이션·예외 코드 | §2, §10, §10-1, §11 | `feat/session-feedback/#595-domain-db` | 없음 |
| B | [#596](https://github.com/depromeet/dpm-core-server/issues/596) | 세션 생성·수정·상세에 피드백 설정 (#1~#3) | §3, §4 | `feat/session-feedback/#596-session-crud` | #595 |
| C | [#597](https://github.com/depromeet/dpm-core-server/issues/597) | 세션 목록 피드백 상태·버튼 노출 (#4) | §5 | `feat/session-feedback/#597-session-list` | #595, #596 |
| D | [#598](https://github.com/depromeet/dpm-core-server/issues/598) | 피드백 화면 진입 API (#5) | §6, §11-4 | `feat/session-feedback/#598-entry-me` | #595 |
| E | [#599](https://github.com/depromeet/dpm-core-server/issues/599) | 피드백 제출 API (#6) | §7, §11-4 | `feat/session-feedback/#599-submit` | #595, #598 |
| F | [#600](https://github.com/depromeet/dpm-core-server/issues/600) | 홈 카드 pending 조회 (#7) | §8 | `feat/session-feedback/#600-home-pending` | #595, #598, #599 |
| G | [#601](https://github.com/depromeet/dpm-core-server/issues/601) | 운영진 인사이트 조회 (#8) | §9 | `feat/session-feedback/#601-insight` | #595, #596, #599 |
| H | [#602](https://github.com/depromeet/dpm-core-server/issues/602) | 수집 시작 PUSH 알림 | §1, §3 | `feat/session-feedback/#602-push-opened` | #595, #596 |

> **병렬 가능**: #595 머지 후 #596∥#598∥#602 일부 가능. #599는 #598 상태 판정 로직 재사용 권장. #600은 #599 이후. #601은 제출(#599)이 있어야 집계 의미가 있음.

### 13-3. 브랜치 / PR 네이밍

| 항목 | 형식 | 예시 |
| --- | --- | --- |
| 브랜치 | `feat/session-feedback/#{N}-{slug}` | `feat/session-feedback/#594-domain-db` |
| PR 제목 | `feat : {이슈 제목 요약}` | `feat : 세션 피드백 도메인·엔티티·마이그레이션` |
| PR body | `Closes #{N}` + Tasks 체크리스트 | 기존 PR #592 형식 |
| 커밋 | `feat : ...` / `fix : ...` / `chore : ...` | 기존 로그 스타일 |

### 13-4. 이슈별 범위 (Tasks 요약)

**A. 도메인·DB·예외**
- enum / 엔티티 3종 / aggregate·port / `SessionFeedbackExceptionCode`
- `db/pending/*_session_feedback.sql` + jOOQ codegen + persistence adapter
- API·컨트롤러 없음

**B. 세션 CRUD (#1~#3)**
- `SessionCreateRequest`/`SessionUpdateRequest`에 `feedbackEnabled`, `feedbackStartAt`, `feedbackPushEnabled`
- 저장 시 `session_feedback_forms` upsert (`endAt = startAt + 72h`)
- `SessionDetailResponse.feedback` (`status`, `startAt`, `endAt`, `pushEnabled`)
- 검증: `400-01`, `400-02`, (안) `409-02`

**C. 세션 목록 (#4)**
- `SessionListDetailResponse.feedback` (`status`, `endAt`, `canSubmit`)
- 로그인 멤버 기준 `canSubmit` (비로그인 정책은 §12-3)

**D. 진입 (#5)** — `GET /v2/sessions/{sessionId}/feedbacks/me`
- `myStatus` + `questions`(AVAILABLE일 때만)
- 상태 화면은 200 (에러 아님). 세션 없음만 `SESSION-404-01`

**E. 제출 (#6)** — `POST /v2/sessions/{sessionId}/feedbacks`
- §11-4 검증 순서 그대로
- unique 충돌 → `409-01`, 응답 200 + data 없음

**F. 홈 카드 (#7)** — `GET /v2/sessions/feedbacks/me/pending`
- `canSubmit` 세션 1건(`endAt` 가장 빠른 것), 없으면 `data` 생략

**G. 인사이트 (#8)** — `GET /v2/sessions/{sessionId}/feedbacks/insight`
- 실시간 집계, 응답자 식별 정보 미포함
- OFF 세션 → `400-03`

**H. PUSH**
- `NotificationMessageType.SESSION_FEEDBACK_OPENED` 추가
- `SessionReminderScheduler`와 같은 패턴으로 `feedbackStartAt` 도달 시 출석·지각자에게 발송
- 중복 방지: `sent_session_notifications` 또는 form의 `push_sent_at`

### 13-5. 이슈 body 템플릿

```markdown
### Describe

{한 줄 요약}

스펙: `docs/SESSION_FEEDBACK_API.md` (§…)

### Tasks

- [ ] …
- [ ] …

### Dependencies

- 선행: #{A}
- 후행: #{E}, #{F}

### ETC

- 브랜치: `feat/session-feedback/#{N}-{slug}`
- base: `develop` (또는 선행 PR 브랜치)
```

### 13-6. 생성된 이슈

| 순서 | Issue | URL | 브랜치 |
| --- | --- | --- | --- |
| A | #595 | https://github.com/depromeet/dpm-core-server/issues/595 | `feat/session-feedback/#595-domain-db` |
| B | #596 | https://github.com/depromeet/dpm-core-server/issues/596 | `feat/session-feedback/#596-session-crud` |
| C | #597 | https://github.com/depromeet/dpm-core-server/issues/597 | `feat/session-feedback/#597-session-list` |
| D | #598 | https://github.com/depromeet/dpm-core-server/issues/598 | `feat/session-feedback/#598-entry-me` |
| E | #599 | https://github.com/depromeet/dpm-core-server/issues/599 | `feat/session-feedback/#599-submit` |
| F | #600 | https://github.com/depromeet/dpm-core-server/issues/600 | `feat/session-feedback/#600-home-pending` |
| G | #601 | https://github.com/depromeet/dpm-core-server/issues/601 | `feat/session-feedback/#601-insight` |
| H | #602 | https://github.com/depromeet/dpm-core-server/issues/602 | `feat/session-feedback/#602-push-opened` |

작업 시작 예:

```bash
git checkout develop
git pull
git checkout -b feat/session-feedback/#595-domain-db
```

PR body에 `Closes #595` 를 넣는다.
