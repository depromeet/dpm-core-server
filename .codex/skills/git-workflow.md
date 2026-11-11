---
name: git-workflow
description: dpm-core-server 커밋 타입 기준, 브랜치 네이밍, PR 본문 구조, 금지 항목.
---

# 커밋 · PR 규칙

## 기본 워크플로

저장소에 남길 변경을 하면 로컬에만 두지 말고 아래 순서로 끝낸다.

1. **브랜치** — `develop` 기준 작업 브랜치 (이미 브랜치면 유지)
2. **커밋** — 관련 파일만 스테이징 (시크릿·덤프·로컬 가이드 제외)
3. **푸시** — `git push -u origin HEAD`
4. **PR** — base `develop`. **사이드바/gh 플래그**로 assignee·reviewer·label 지정 후 PR 생성, URL 전달
   - `--assignee "@me"` / `--reviewer hwistlezz --reviewer cowboysj` / `--label "커밋 타입에 맞는 레이블"`
   - 본문에 reviewer·assignee `@멘션` 하지 않는다

사용자가 "커밋만", "PR 올리지 마", "로컬만"이라고 **명시할 때만** PR을 생략한다.  
"적용해줘 / 수정해줘"만 있으면 **커밋 + PR까지** 하는 것이 기본이다.

---

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

1. 이슈를 새로 파거나 기존 이슈에 묶는 작업이면 **이슈를 먼저** 만든다.
2. PR Summary에 반드시 closing keyword로 연결: `Closes #이슈번호`
   - `#123`만 쓰면 링크만 되고 이슈가 닫히지 않는다. `Closes` / `Fixes` / `Resolves` 사용.
   - `develop`으로 머지될 때 이슈가 닫힌다.
   - 이슈 없이 작은 수정만이면 이슈는 생략해도 된다.
3. PR 베이스: `develop`.
4. `main`/`master` force push 금지.
5. PR 생성 시 **label**과 **assignee**를 반드시 지정한다.

---

## Label

커밋 타입과 대응하는 레이블을 선택한다.

| 레이블 | 커밋 타입 | 사용 기준 |
|---|---|---|
| `✨ Feature` | `feat` | 새 기능 추가 |
| `🐞 Bugfix` | `fix` | 버그 수정·핫픽스 |
| `🔨 Refactor` | `refactor` | 동작 변경 없는 코드 개선 |
| `📑 Docs` | `docs` / `chore` | 문서, 설정, 의존성 변경 |
| `🔀 Deploy` | `ci` | 릴리스·운영 배포·CD 파이프라인 |
| `🙋 Suggestion` | — | 논의·제안이 필요한 PR |

`gh pr create` 시:
```bash
gh pr create --label "✨ Feature" ...
```

여러 개 가능: `--label "✨ Feature" --label "📑 Docs"`

---

## Reviewer · Assignee · Label

Reviewer / Assignee / Label은 **반드시** `gh pr create` 플래그 또는 GitHub 사이드바로 지정한다.  
PR 본문에 `@멘션`으로 넣지 않는다.

```bash
# 기본 형태
gh pr create \
  --base develop \
  --assignee "@me" \
  --reviewer hwistlezz \
  --reviewer cowboysj \
  --label "✨ Feature" \
  --title "feat: ..." \
  --body "..."

# 작성자가 hwistlezz 또는 cowboysj 인 경우
# → 본인은 reviewer로 요청 불가 → 나머지 한 명만
gh pr create --assignee "@me" --reviewer cowboysj --label "🐞 Bugfix" ...
```

고정 Reviewer: `hwistlezz`, `cowboysj`  
Assignee: `@me` (항상 본인)

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

- **Reviewer / Assignee `@멘션`** — `gh --reviewer` / `--assignee` 플래그 또는 사이드바로만
- **"리뷰어 후보: @xxx"** 같은 문구 — PR 본문에 절대 포함 금지
- Figma 링크
- Breaking change 전용 섹션
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
