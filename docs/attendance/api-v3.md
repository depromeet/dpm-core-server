# 출석·세션 API v3 경로

#573(출석 시간·정합성·자동 결석), #579(출석 현황), #580(이미지), #583(사유서 이미지)에서 추가됐거나 요청·응답·판정이 바뀐 API 만 `/v3` 로 옮겼다.
옛 경로는 남기지 않는다(별칭 없음). 아래에 없는 API 는 원래 경로 그대로다. 권한(`@PreAuthorize`)은 바뀌지 않았다.

| 메서드 | 이전 | v3 | 권한 | 바뀐 점 (PR) |
|---|---|---|---|---|
| POST | `/v1/sessions` | `/v3/sessions` | `create:session` | 출석 시각 3개 생략 시 서버 기본값, 순서 오류 `SESSION-400-08`, 일부만 입력 `SESSION-400-09` (#575). 출석 기록을 같은 트랜잭션에서 만들고 멤버 없는 기수도 성공 (#576) |
| PATCH | `/v1/sessions` | `/v3/sessions` | `update:session` | 순서 검증 (#575). 같은 트랜잭션에서 재판정하고 운영진 변경 기록 보존 (#576). 마감 연장 시 자동 결석을 PENDING 으로 되돌림 (#578) |
| PATCH | `/v1/sessions/{sessionId}/attendance-time` | `/v3/sessions/{sessionId}/attendance-time` | `update:session` | 같은 날짜 검증 대신 순서 검증(`SESSION-400-08`), 날짜가 달라도 됨 (#575) |
| PATCH | `/v1/sessions/{sessionId}/delete` | `/v3/sessions/{sessionId}/delete` | `delete:session` | 출석 기록을 같은 트랜잭션에서 함께 삭제 (#576) |
| GET | `/v1/sessions/{sessionId}/update-policy` | `/v3/sessions/{sessionId}/update-policy` | `update:session` | 실제 반영과 같은 재판정 규칙으로 대상 계산 (#576) |
| POST | `/v1/sessions/{sessionId}/attendances` | `/v3/sessions/{sessionId}/attendances` | `create:attendance` | 마감 후 거절 `SESSION-400-06`, 운영진 확정 시 `SESSION-400-07` (#576). 마감 전 요청은 자동 결석보다 늦게 저장돼도 정상 판정 (#578) |
| PATCH | `/v1/sessions/{sessionId}/attendances/{memberId}` | `/v3/sessions/{sessionId}/attendances/{memberId}` | `update:attendance` | 세션 잠금 후 조건부 변경, 없는 세션은 세션 404 (#576) |
| PATCH | `/v1/sessions/{sessionId}/attendances/bulk` | `/v3/sessions/{sessionId}/attendances/bulk` | `update:attendance` | 하나라도 없으면 전부 미반영, 중복 id 무시, 빈 목록은 아무것도 안 함, 없는 세션은 세션 404 (#576) |
| GET | `/v1/members/attendances` | `/v3/members/attendances` | `create:attendance` | 수료 판정(`attendanceStatus`) 규칙 변경, 상태 필터가 대상 멤버만 고름 (#579) |
| GET | `/v1/sessions/{sessionId}/attendances/{memberId}` | `/v3/sessions/{sessionId}/attendances/{memberId}` | `create:attendance` | 수료 판정 규칙 변경 (#579) |
| GET | `/v1/members/{memberId}/attendances` | `/v3/members/{memberId}/attendances` | `update:member` | `earlyLeaveCount` 제거, `sessions[].isOnline`·`absenceReason` 추가, 수료 판정 변경 (#579). `absenceReason.imageIds` 추가 (#583) |
| GET | `/v1/members/me/attendances` | `/v3/members/me/attendances` | `read:attendance` | 위와 같음 (#579, #583) |
| POST | `/v2/sessions/{sessionId}/absence-reasons` | `/v3/sessions/{sessionId}/absence-reasons` | `create:attendance` | `imageIds`, 50자 초과 `ATTENDANCE-400-03`, 이미지 오류 400/409 (#583) |
| PATCH | `/v2/sessions/{sessionId}/absence-reasons` | `/v3/sessions/{sessionId}/absence-reasons` | `create:attendance` | 위와 같음 (#583) |
| DELETE | `/v2/sessions/{sessionId}/absence-reasons` | `/v3/sessions/{sessionId}/absence-reasons` | `create:attendance` | 첨부 링크 함께 삭제 (#583) |
| PATCH | `/v2/sessions/{sessionId}/absence-reasons/{memberId}/review` | `/v3/sessions/{sessionId}/absence-reasons/{memberId}/review` | `update:attendance` | 세션 잠금, 없는 세션이면 세션 404 (#583) |
| GET | `/v2/sessions/{sessionId}/absence-reasons/me` | `/v3/sessions/{sessionId}/absence-reasons/me` | `create:attendance` | `imageIds` 추가 (#583) |
| GET | `/v2/sessions/{sessionId}/absence-reasons` | `/v3/sessions/{sessionId}/absence-reasons` | `update:attendance` | `reasons[].imageIds` 추가 (#583) |
| GET | 없음 | `/v3/sessions/{sessionId}/absence-reasons/{memberId}/images/{imageId}` | `update:attendance` | 새 API. 운영진용 이미지 조회 URL (#583) |
| POST | 없음 | `/v3/images/uploads` | 로그인 | 새 API. 직접 업로드 URL 발급 (#580) |
| POST | 없음 | `/v3/images/uploads/{uploadId}/complete` | 로그인 | 새 API. 업로드 완료 확인 (#580) |
| GET | 없음 | `/v3/images/{imageId}` | 로그인 | 새 API. 본인 이미지 조회 URL (#580) |

`/v3/**` 는 `/v1/**`·`/v2/**` 와 같이 URL 단계에서 통과시키고(SecurityConfig), 권한은 메서드의 `@PreAuthorize` 가 본다. CORS 도 같은 설정을 쓴다.

자동 결석 스케줄러(#578)는 API 가 아니라 경로가 없다.

## 그대로인 API

`GET /v1/sessions`, `/v1/sessions/next`, `/v1/sessions/weeks`, `/v1/sessions/{sessionId}`, `/v1/sessions/{sessionId}/me`,
`/v1/sessions/{sessionId}/attendance-time`, `/v1/sessions/{sessionId}/attendances`, `/v1/sessions/{sessionId}/attendances/me`.
