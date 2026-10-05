# 결석 사유서 이미지 첨부 (#583)

결석 사유서 1건에 이미지 N장을 순서대로 붙인다. 이미지는 비공개 저장소에 있고(`docs/images/oci-object-storage.md`), 공개 URL 은 없으며 권한 확인 후 만료되는 조회 URL 만 내준다.

## 경로

사유서 API 는 모두 `/v3` 로 옮겼다(제출·수정·삭제·검토·조회, 이미지 조회 URL). 예전 `/v2/sessions/{sessionId}/absence-reasons...` 경로는 없다.

## 흐름

이미지는 한 장씩 저장소에 직접 올리고, 처리가 끝난 `imageId` 만 사유서에 붙인다.

1. `POST /v3/images/uploads` 에 `{ "contentType": "image/png", "size": 12345 }` 를 보내 `uploadId`, `uploadUrl`, `expiresAt` 을 받는다.
2. 파일 원본을 `uploadUrl` 로 `PUT` 한다. 저장소로 바로 가는 요청이므로 백엔드 인증 헤더를 붙이지 않는다. `expiresAt` 이 지나면 1부터 다시 한다.
3. `POST /v3/images/uploads/{uploadId}/complete` 를 부른다.
   - `202`: 아직 처리 중. `Retry-After` 초만큼 기다렸다가 같은 요청을 다시 보낸다.
   - `200`: 준비 완료. `{ "imageId", "contentType", "size" }` 를 받는다.
4. `200` 으로 받은 `imageId` 만 `POST` 또는 `PATCH /v3/sessions/{sessionId}/absence-reasons` 의 `imageIds` 에 표시 순서대로 담는다.

```json
{ "contents": "병원 진료", "imageIds": [12, 15] }
```

| imageIds | 동작 |
|---|---|
| 생략 / `null` | 기존 첨부 유지 (새 사유서는 첨부 없음). 이미지 기능 전 클라이언트는 그대로 동작 |
| `[]` | 모두 해제 |
| `[15, 12]` | 이 순서로 교체 (같은 이미지로 순서만 바꿔도 된다) |

- 본인이 올린 이미지만, 중복 없이. 개수 상한은 없다.
- 이미지 하나는 한 사유서에만 붙는다. 해제하거나 사유서를 지우면 다른 사유서에 다시 붙일 수 있다.
- 내용·상태(PENDING 으로 재제출)·첨부는 한 트랜잭션으로 저장된다. 하나라도 거절되면 아무것도 바뀌지 않는다.
- 해제/삭제해도 이미지와 저장소 객체는 지우지 않는다.

## 오류

| 코드 | 경우 |
|---|---|
| 400 `ATTENDANCE-400-03` | 사유 50자 초과 |
| 400 `ATTENDANCE-400-04` | 없는 이미지, 남의 이미지, 0 이하/`null` id (서로 구분하지 않음) |
| 400 `ATTENDANCE-400-05` | 같은 id 중복 |
| 409 `ATTENDANCE-409-02` | 다른 사유서에 이미 붙은 본인 이미지 (동시 요청 포함) |

## 조회

- `imageIds` (순서, 없으면 `[]`): `GET /v3/sessions/{sessionId}/absence-reasons/me`, 운영진 `GET /v3/sessions/{sessionId}/absence-reasons`,
  `GET /v3/members/{memberId}/attendances`·`/v3/members/me/attendances` 의 `sessions[].absenceReason`
- 조회 URL (`CustomResponse` 의 `data` 가 `{ "url", "expiresAt" }`)
  - 본인: `GET /v3/images/{imageId}` (소유자 전용)
  - 운영진(`update:attendance`): `GET /v3/sessions/{sessionId}/absence-reasons/{memberId}/images/{imageId}`
    그 세션·멤버의 사유서에 지금 붙어 있고 그 멤버가 올린 이미지만. 아니면 `IMAGE-404-01`. 확인을 마친 뒤에만 저장소에 URL 을 요청한다.
  - 둘 다 `Cache-Control: no-store, private`.

```json
{ "status": "OK", "message": "요청에 성공했습니다", "code": "G000",
  "data": { "url": "https://...", "expiresAt": "2026-10-04T03:20:00Z" } }
```

`url` 은 인증 헤더 없이 `img` 등에 바로 쓴다. `expiresAt` 이 지나면 `url` 은 더 이상 열리지 않으므로 같은 조회 API 를 다시 불러 새 `url` 을 받는다.
`url` 을 저장하거나 캐시하지 말고 `imageId` 를 기준으로 그때그때 받는다.

## 동시성

- 제출/수정/삭제/검토는 세션 행 `FOR UPDATE` 를 먼저 잡는다(잠금 순서 세션 → 출석 → 사유서). 같은 세션의 재제출·첨부 변경이 직렬화되고 새 중복 사유서가 생기지 않는다.
- 교체는 기존 링크를 PK 로 지운 뒤 `image_id` 오름차순으로 넣는다. 다른 세션 사유서와 같은 이미지를 동시에 붙이면 `UNIQUE(image_id)` 가 막고, 롤백 후 409 로 바뀐다.

## 배포

`db/pending/2610031200_absence_reason_images.sql` 을 먼저 적용한다(validate).
