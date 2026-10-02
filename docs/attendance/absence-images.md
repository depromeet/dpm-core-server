# 결석 사유서 이미지 첨부 (#583)

결석 사유서 1건에 이미지 N장을 순서대로 붙인다. 이미지는 비공개 저장소에 있고(`docs/images/oci-object-storage.md`), 공개 URL 은 없다.

## 흐름

1. `POST /v1/images` 로 한 장씩 올려 `imageId` 를 받는다.
2. `POST` 또는 `PATCH /v2/sessions/{sessionId}/absence-reasons` 에 `imageIds` 를 표시 순서대로 담는다.

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

- `imageIds` (순서, 없으면 `[]`): `GET /v2/sessions/{sessionId}/absence-reasons/me`, 운영진 `GET /v2/sessions/{sessionId}/absence-reasons`,
  `GET /v1/members/{memberId}/attendances`·`/v1/members/me/attendances` 의 `sessions[].absenceReason`
- 원본
  - 본인: `GET /v1/images/{imageId}` (소유자 전용, 변경 없음)
  - 운영진(`update:attendance`): `GET /v2/sessions/{sessionId}/absence-reasons/{memberId}/images/{imageId}`
    그 세션·멤버의 사유서에 지금 붙어 있고 그 멤버가 올린 이미지만. 아니면 `IMAGE-404-01`. 확인을 마친 뒤에만 저장소를 읽는다.
  - 둘 다 `Cache-Control: no-store, private`, `nosniff`, 임의 UUID 파일명. 인증 헤더가 필요하므로 fetch → Blob URL 로 표시한다.

## 동시성

- 제출/수정/삭제/검토는 세션 행 `FOR UPDATE` 를 먼저 잡는다(잠금 순서 세션 → 출석 → 사유서). 같은 세션의 재제출·첨부 변경이 직렬화되고 새 중복 사유서가 생기지 않는다.
- 교체는 기존 링크를 PK 로 지운 뒤 `image_id` 오름차순으로 넣는다. 다른 세션 사유서와 같은 이미지를 동시에 붙이면 `UNIQUE(image_id)` 가 막고, 롤백 후 409 로 바뀐다.

## 배포

`db/pending/2610031200_absence_reason_images.sql` 을 먼저 적용한다(validate).
