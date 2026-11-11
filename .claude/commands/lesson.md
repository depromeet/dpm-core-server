---
name: lesson
description: 교정 사항을 practice 규칙으로 변환하고 저장 위치를 결정한다. 사용자가 AI를 교정했거나 "이거 다음에 기억해"라고 했을 때 사용.
---

# /lesson

교정 사항을 재발 방지 규칙으로 만든다. 직접 `/lesson`을 치거나, AI가 같은 실수를 두 번 하면 AI가 먼저 제안한다.

---

## 실행 흐름

### 1단계: practice 한 줄 도출

교정 내용을 아래 형식으로 요약한다:

```
✗ [잘못한 것] → ✓ [올바른 것]
```

**예시:**
```
✗ AttendanceService에서 JPA Entity 직접 import → ✓ Port 인터페이스만 의존
✗ 출석 상태를 PENDING → EXCUSED_ABSENT 직접 전환 → ✓ 결석사유서 제출 경유 필수
✗ PR base 브랜치를 main으로 설정 → ✓ develop으로 설정
✗ 커밋 메시지에 "Co-authored-by Cursor" 포함 → ✓ 해당 문구 삽입 안 함
```

일회성 오타·변수명 수정은 여기서 멈추고 종료한다.

### 2단계: 기존 규칙과 충돌 확인

다음 파일에서 비슷한 규칙이 이미 있는지 확인한다:
- `CLAUDE.md`
- `.claude/memory/project.md`
- 관련 스킬 파일 (`.claude/skills/`)

겹치면 "기존 규칙 X와 충돌합니다 — 덮어쓸까요 / 병합할까요 / 패스할까요?" 묻는다.

### 3단계: 저장 위치 선택

다음 형식으로 사용자에게 묻는다:

```
📌 규칙 후보:
✗ [잘못한 것] → ✓ [올바른 것]

어디에 저장할까요?
  a) project.md  — 이 프로젝트 세션 메모 (팀 미공유)
  b) CLAUDE.md   — 팀 공유 규칙 (커밋 대상)
  c) 스킬 승격   — .claude/skills/<파일>.md
  d) 패스        — 저장 안 함
```

### 4단계: 저장

**a 선택 → `.claude/memory/project.md` append:**
```
- 2026-01-15: ✗ PR base를 main으로 → ✓ develop으로
```

**b 선택 → `CLAUDE.md` 관련 섹션에 한 줄 추가.**  
섹션이 없으면 "## 교정 규칙" 생성 후 추가.

**c 선택 → 관련 스킬 파일에 추가.**  
어느 스킬인지 모르면 "어느 스킬에 넣을까요?" 묻는다.  
스킬이 없으면 새로 만들지 확인한다.

**d 선택 → 종료.**

---

## 대화 예시

### 사용자가 직접 호출

```
사용자: /lesson
         방금 내가 "PR base 브랜치는 develop이라고 했잖아"라고 교정했음

AI:    📌 규칙 후보:
       ✗ PR base 브랜치를 main으로 설정 → ✓ develop으로 설정

       어디에 저장할까요?
         a) project.md  — 이 프로젝트 세션 메모 (팀 미공유)
         b) CLAUDE.md   — 팀 공유 규칙 (커밋 대상)
         c) 스킬 승격   — .claude/skills/git-workflow.md
         d) 패스        — 저장 안 함

사용자: c

AI:    .claude/skills/git-workflow.md 의 "PR 흐름" 섹션에 추가했습니다.
```

### AI가 먼저 제안

```
AI:    (같은 실수를 두 번 한 직후)
       방금 PR base를 main으로 설정하려 했는데 두 번째입니다.
       /lesson 으로 규칙화할까요?

사용자: 응

AI:    📌 규칙 후보:
       ✗ PR base 브랜치를 main으로 설정 → ✓ develop으로 설정
       ...
```

---

## a / b / c / d 판단 기준

| 상황 | 선택 |
|---|---|
| 이 프로젝트 고유 사항. 다른 팀원도 알아야 함 | **b** (CLAUDE.md) |
| 헥사고날·API·테스트 등 패턴 계열 규칙 | **c** (관련 스킬) |
| 개인 발견이거나 아직 팀 합의 전 | **a** (project.md) |
| 맥락 없이 한 번 실수, 재발 가능성 낮음 | **d** (패스) |
| 개인 설정·시크릿·로컬 경로 | CLAUDE.local.md (별도, 직접 편집) |

---

## 하지 말 것

- 승인 없이 CLAUDE.md·skills에 직접 쓰지 않는다.
- 오타 수정·변수명 변경 같은 일회성 작업은 `/lesson`을 열지 않는다.
- practice를 두 줄 이상으로 쓰지 않는다 — 한 줄 원칙.
- 저장 후 커밋까지 자동으로 하지 않는다 (사용자가 원할 때만).
