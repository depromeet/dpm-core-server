# PR / Issue 작성 가이드

이 저장소에서 PR·Issue를 만들 때 따르는다.  
템플릿 원본: [PULL_REQUEST_TEMPLATE.md](./PULL_REQUEST_TEMPLATE.md), [ISSUE_TEMPLATE/](./ISSUE_TEMPLATE/)

> Cursor용 `.mdc` 규칙 파일은 **이 문서(`.md`)와 같이 커밋하지 않는다.**  
> 공유 규칙은 이 `.md`만 저장소에 둔다. `.mdc`는 로컬(`.cursor/rules/`, gitignore)에만 둔다.

## Issue

- Feature면 `.github/ISSUE_TEMPLATE/feature.yml` 형식 사용
- 섹션: `### Describe` / `### Tasks` / `### ETC`
- 라벨: `✨ Feature` (버그·리팩터는 해당 템플릿 라벨)
- PR보다 **먼저** 이슈를 만들고, PR Summary에 `#이슈번호`로 연결

## PR 본문 구조 (필수)

```markdown
## Summary

> - #이슈번호

한두 문장 요약.

## Tasks

- 이 PR에 포함된 작업 목록

## API

<!-- API가 바뀐 경우에만. 없으면 섹션 삭제 -->

### `METHOD /path`

- Auth: …
- Request / Response 예시(JSON) 또는 필드 표
- 상태값·분기 표 (있을 때)

## ETC

<!-- 추가 정보. 없으면 짧게 또는 삭제 -->

## Screenshot

<!-- UI 관련일 때만. 없으면 섹션 삭제 -->
```

## 넣지 말 것

- **Figma 링크** (피그마 URL을 PR/Issue에 넣지 않음. 화면은 Screenshot으로만)
- **Breaking change** 같은 별도 경고 섹션 (필요하면 ETC·API에 자연스럽게 녹임)
- `Made with Cursor` / Co-authored-by Cursor
- 커밋·PR 작성자에 Cursor를 넣지 않음

## API 섹션 (API 변경 시 필수)

변경된 엔드포인트마다:

1. Method + path
2. Auth
3. 요청/응답 스펙 (JSON 예시 또는 표)
4. FE가 알아야 할 상태·필드 분기

예:

```markdown
## API

### `GET /v1/sessions/next`

- Auth: `isAuthenticated()`
- Response `data`:

\`\`\`json
{ "status": "AVAILABLE", "cohortValue": "19", "session": { } }
\`\`\`

| status | 의미 |
|--------|------|
| AVAILABLE | … |
```

## Screenshot

- 피그마/실기기 화면을 이미지로 첨부 (링크만 넣지 않음)
- **여러 장**: 마크다운 **표**로 나란히. 셀 안 이미지는 `width="200"` 전후

```markdown
## Screenshot

| AVAILABLE | NOT_REGISTERED | COHORT_ENDED |
|:---------:|:--------------:|:------------:|
| <img src=".../a.png" width="200" /> | <img src=".../b.png" width="200" /> | <img src=".../c.png" width="200" /> |
```

- **한 장**: 표 없이 넣고 **작게** (`width="280"` 전후)

```markdown
## Screenshot

<img src=".../single.png" width="280" />
```

- 이미지는 브랜치에 두고 raw URL을 쓰거나, GitHub 업로드 URL을 사용
  - 예: `https://raw.githubusercontent.com/<org>/<repo>/<branch>/docs/.../shot.png`

## 커밋·푸시

- 커밋 메시지 스타일은 최근 로그를 따름 (`feat : …`, `docs : …` 등)
- Cursor co-author trailer 없이 작성자만 남김
- 문서/스크린샷만 추가할 때도 같은 규칙

## 체크리스트

- [ ] Issue 생성 후 PR Summary에 `#번호`
- [ ] Summary / Tasks / (API) / (ETC) / (Screenshot) 템플릿
- [ ] API 변경 시 스펙 포함
- [ ] Figma 링크 없음
- [ ] 스크린샷: 다수=표, 단일=축소
- [ ] Cursor 문구·Co-authored-by 없음
