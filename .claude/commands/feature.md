---
name: feature
description: 요구사항을 받아 질문 → 계획 → 구현 → PR까지 자동으로 진행하는 피처 개발 워크플로우.
---

# /feature — 피처 개발 워크플로우

사용자가 요구사항을 주면 스스로 파악 가능한 것은 파악하고, 꼭 필요한 것만 물어본 뒤 PR까지 진행한다.

---

## 1단계: 요구사항 파악

요구사항에서 아래 항목을 먼저 스스로 추론한다.

**스스로 파악할 것:**
- 도메인 — 요구사항 키워드로 추론 (출석/세션/피드백/기수/멤버 등)
- 영향 레이어 — API 추가인지 도메인 로직인지 DB인지
- 다기수 영향 — 복수 기수 멤버가 접근하는 데이터인지
- DB 변경 필요 여부 — 신규 컬럼·테이블 필요 여부

**Figma URL이 있으면:**
Figma MCP(`mcp__figma__get_figma_data`)로 해당 프레임을 열어 UI 구조·필드·상태를 분석한 뒤 요구사항으로 변환한다.

---

## 2단계: 질문 (꼭 필요한 것만)

추론이 불확실한 항목만 한 번에 모아서 묻는다. 항목별로 나눠서 여러 번 묻지 않는다.

**반드시 묻는 항목 (추론 불가):**

```
📋 확인이 필요합니다:

1. git worktree를 따서 작업할까요? (병렬 작업 중이라면 필요)
   → 예라면 워크트리 경로를 지정해주세요. (예: ~/worktrees/feature-xxx)
   → 현재 실행 중인 서버가 있다면 포트 충돌을 피하기 위해 포트를 조정합니다.

2. PR까지 올릴까요, 아니면 로컬 커밋까지만 할까요?

[추론이 불확실한 항목만 추가]
```

---

## 3단계: 계획 제시 → 승인

답변을 받은 뒤 구현 계획을 보여준다.

```
📐 구현 계획:

브랜치: feat/<짧은-설명>
도메인: <파악한 도메인>

[생성·수정할 파일 목록]
- domain/.../Xxx.kt                  — 도메인 객체·Port
- application/.../XxxCommandService.kt
- application/.../XxxQueryService.kt
- application/.../XxxCommandController.kt + XxxCommandApi.kt
- persistence/.../XxxRepository.kt
- entity/.../XxxEntity.kt            — (필요 시)
- db/pending/YYMMDDHHMM_설명.sql      — (DB 변경 있을 때)

[테스트 파일]
- application/src/test/.../XxxCommandServiceTest.kt
- application/src/test/.../core/it/XxxMySqlIntegrationTest.kt  — (통합 필요 시)

진행할까요? (y / 수정 사항)
```

사용자가 `y` 또는 별다른 수정 없이 답하면 구현 시작.

---

## 4단계: 구현

아래 순서로 진행한다.

1. **브랜치 생성** (worktree 여부에 따라)
   - worktree: `.claude/skills/git-worktree.md` 참조
   - 일반: `git checkout -b feat/<설명> develop`

2. **도메인 → 퍼시스턴스 → 애플리케이션 순으로 구현**
   - 헥사고날 규칙 준수 (`.claude/skills/hexagonal-arch.md`)
   - API 작성 패턴 준수 (`.claude/skills/api-design.md`)

3. **DB 변경이 있으면** `db/pending/` 에 SQL 파일 작성
   - `.claude/skills/db-migration.md` 참조

4. **테스트 작성** (`.claude/skills/testing-patterns.md`)

5. **다기수 영향이 있으면** `.claude/skills/multi-cohort-scenarios.md` 체크리스트 확인

---

## 5단계: PR 생성

PR까지 올리기로 했다면:

```bash
gh pr create \
  --base develop \
  --assignee "@me" \
  --reviewer hwistlezz \
  --reviewer cowboysj \
  --label "<커밋 타입에 맞는 label>" \
  --title "<type>: <설명>" \
  --body "..."
```

PR 본문은 `.claude/skills/git-workflow.md` 의 PR 본문 구조 참조.

---

## 하지 말 것

- 추론 가능한 것을 질문하지 않는다 (도메인, 레이어 등).
- 질문을 여러 번으로 나눠서 하지 않는다 — 한 번에 모은다.
- 승인 없이 구현을 시작하지 않는다.
