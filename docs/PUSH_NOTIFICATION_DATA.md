# Expo 푸시 알림 (실제로 발송되는 것만)

## 제목·본문은 누가 정하나?

**서버에서 정합니다.**  
`NotificationMessageType` 의 `title` / `bodyTemplate` 을 서버가 채워서 Expo로 보냅니다.  
프론트는 푸시를 **받아** `data.type` + id 로 화면 이동만 하면 됩니다. (문구를 FE가 고르지 않음)

예외: 관리자/테스트용 커스텀 푸시 API, 뒤풀이 RSVP 미제출 리마인드(`sendCustom*`)는 호출 시 넘긴 title/body를 그대로 쓰며 **`type` 없음**.

`messageType` 발송 시 `data` 에는 항상 `"type": "<enum 이름>"` 이 붙는다.

---

## 실제로 가는 알림

| `type` | 제목 (서버) | 본문 요지 (서버) | `data` id |
|---|---|---|---|
| `ANNOUNCEMENT_NEW` | 새로운 공지가 등록됐어요. | `{title}` | `announcementId` |
| `ANNOUNCEMENT_REMIND` | 아직 읽지 않은 공지가 있어요. | `{title}` | `announcementId` |
| `ASSIGNMENT_NEW` | 새로운 과제가 등록됐어요. | `{title}` | `announcementId` |
| `ASSIGNMENT_SUBMIT_REQUEST` | 아직 제출하지 않은 과제가 있어요. | `{title}` | `announcementId` |
| `ASSIGNMENT_SUBMIT_REQUEST_TO_MEMBERS` | 과제를 제출해주세요! | `{title}` | `announcementId` |
| `ASSIGNMENT_DUE_24H` | 과제 제출까지 하루 남았어요. | 마감 안내 고정 문구 | `announcementId` |
| `ASSIGNMENT_DUE_12H` | 과제 제출까지 하루도 남지 않았어요. | 마감 안내 고정 문구 | `announcementId` |
| `ASSIGNMENT_DUE_1H` | 제출까지 한시간도 남지 않았어요 | 마감 안내 고정 문구 | `announcementId` |
| `SESSION_DAY_BEFORE` | 내일 진행되는 세션 정보를 확인해주세요. | `{title}` (세션명) | `sessionId` |
| `ATTENDANCE_STARTED` | 출석체크가 시작됐어요. | 코드 확인·출석 안내 | `sessionId` |
| `SESSION_FEEDBACK_OPENED` | 세션 피드백이 시작됐어요. | `{title} 세션은 어떠셨나요? …` | `sessionId` |
| `AFTER_PARTY_INVITATION` | 새로운 회식이 열렸어요. | `{title}` | `afterPartyId` |
| `ABSENCE_REASON_SUBMITTED` | 새로운 결석 사유서가 제출됐어요. | `{name}님이 {week}주차 …` | `sessionId`, `memberId` |

### 예시

```json
{ "type": "SESSION_FEEDBACK_OPENED", "sessionId": 12 }
{ "type": "ANNOUNCEMENT_NEW", "announcementId": 34 }
{ "type": "AFTER_PARTY_INVITATION", "afterPartyId": 7 }
{ "type": "ABSENCE_REASON_SUBMITTED", "sessionId": 12, "memberId": 56 }
```

---

## enum 에만 있고 typed 발송 없음 (참고)

`SESSION_START_SOON`, `SESSION_STARTED`, `AFTER_PARTY_REMIND` — 정의만 있거나 커스텀 발송. 위 표에 없음.
