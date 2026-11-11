---
name: git-workflow
description: dpm-core-server 커밋 타입 기준, 브랜치 네이밍, PR 본문 구조, 금지 항목(Cursor co-author 등).
---

# 커밋 · PR 규칙

## 커밋 메시지

```
<type>: <한 줄 설명>
```

| type | 사용 기준 | 예시 |
|---|---|---|
| `feat` | 새 기능 추가 | `feat: 세션 피드백 폼 개설 API 추가` |
| `fix` | 버그 수정 | `fix: LATE 출석자 피드백 푸시 미발송 수정` |
| `refactor` | 동작 변경 없는 코드 개선 | `refactor: AttendanceQueryService 쿼리 최적화` |
| `docs` | 문서만 변경 | `docs: 출석 API 스펙 업데이트` |
| `test` | 테스트 추가·수정 | `test: AttendanceCommandService 단위 테스트 추가` |
| `chore` | 빌드·설정·의존성 | `chore: ktlint 버전 업그레이드` |
| `ci` | CI/CD 파이프라인 | `ci: prod CD SSH 시크릿 정합` |

규칙:
- 본문이 필요한 경우 빈 줄로 구분.
- `Co-authored-by: Cursor` / `Made with Cursor` 절대 추가 금지 (`AGENTS.md` 참조).

---

## 브랜치 네이밍

```
feat/<짧은-설명>
fix/<짧은-설명>
refactor/<짧은-설명>
chore/<짧은-설명>
```

예: `feat/session-feedback-push`, `fix/attendance-late-push-bug`

---

## PR 흐름

1. 이슈 먼저 생성 → PR Summary에 `Closes #이슈번호` 연결.
2. PR 베이스: `develop`.
3. `main`/`master` force push 금지.
4. 비긴급 머지 금지 기간 있을 수 있음 — 확인 후 작업.

---

## PR 본문 구조

```markdown
## Summary

> - Closes #이슈번호

한두 문장 요약.

## Tasks

- [ ] 작업 항목 1
- [ ] 작업 항목 2

## API

<!-- API가 바뀐 경우에만. 없으면 섹션 삭제 -->

### `GET /v3/sessions/{sessionId}/attendances`

- Auth: `update:attendance`
- Response: `SessionRosterResponse`

## ETC

## Screenshot

```

---

## 넣지 말 것

- Figma 링크
- `Made with Cursor` / `Co-authored-by Cursor`
- `.md` / `.mdc` / `.github` 가이드 파일을 기능 PR에 같이 커밋
- 스크린샷 이미지 파일을 브랜치에 커밋 (GitHub 드래그 첨부 사용)

---

## 스크린샷 첨부

GitHub PR 본문에 드래그/붙여넣기 → `https://github.com/user-attachments/assets/...` 링크 생성.  
여러 장은 표로, 한 장은 `width="280"` 으로 축소.

```markdown
| 상태A | 상태B |
|:-----:|:-----:|
| <img src="..." width="200" /> | <img src="..." width="200" /> |
```
