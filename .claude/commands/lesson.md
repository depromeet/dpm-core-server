---
name: lesson
description: 교정 사항을 practice 규칙으로 변환하고 저장 위치를 결정한다. 사용자가 AI를 교정했거나 "이거 다음에 기억해"라고 했을 때 사용.
---

# /lesson — 교정 → 규칙화 흐름

이 커맨드는 방금 발생한 교정을 재발 방지 규칙으로 만든다.

## 실행 흐름

### 1단계: practice 한 줄로 재작성

교정 내용을 아래 형식으로 한 줄 요약한다:

```
✗ [잘못한 것] → ✓ [올바른 것]
```

예:
```
✗ AttendanceService에서 JPA Entity 직접 import → ✓ Port 인터페이스만 의존
✗ 출석 상태를 PENDING → EXCUSED_ABSENT 직접 전환 → ✓ 결석사유서 제출 경유 필수
```

일회성 오타·이름 수정은 규칙화하지 않고 즉시 종료한다.

### 2단계: 충돌 확인

저장하기 전에 아래 파일에서 비슷한 규칙이 이미 있는지 확인한다:
- `CLAUDE.md`
- `.claude/memory/project.md`
- 관련 스킬 파일 (`.claude/skills/`)

충돌 발견 시: "기존 규칙과 겹칩니다 — 덮어쓸까요 / 병합할까요 / 패스할까요?" 묻는다.

### 3단계: 저장 위치 선택 (사용자에게 묻기)

```
이 규칙을 어디에 저장할까요?

a) 이 프로젝트만 — .claude/memory/project.md
b) 팀 공유 — CLAUDE.md (커밋 대상)
c) 스킬 승격 — .claude/skills/<관련스킬>.md
d) 패스 — 저장 안 함
```

### 4단계: 저장

**a) project.md 선택 시**
`.claude/memory/project.md` 에 아래 형식으로 append:
```
- YYYY-MM-DD: [practice 한 줄]
```

**b) CLAUDE.md 선택 시**
관련 섹션에 한 줄 추가. 섹션이 없으면 "## 교정 규칙" 섹션 생성.

**c) 스킬 승격 선택 시**
관련 스킬 파일의 적절한 위치에 추가. 없으면 새 스킬 생성 여부 확인.

**d) 패스**
저장 없이 종료.

---

## 저장 위치 가이드

| 발견 종류 | 넣을 곳 |
|---|---|
| 도메인 규칙 ("운영진은 웹 출석 불가") | domain-model 스킬 또는 project.md |
| API 패턴 (CustomResponse, Controller 분리) | api-design 스킬 또는 CLAUDE.md |
| 테스트 패턴 (Fake 포트, mysql-integration) | testing-patterns 스킬 |
| 커밋·PR 규칙 | git-workflow 스킬 또는 CLAUDE.md |
| SSH 경로·비밀번호·개인 설정 | CLAUDE.local.md 만 |
| 반복성 낮은 단발 수정 | 패스 |

---

## 하지 말 것

- 승인 없이 CLAUDE.md·skills에 쓰지 않는다.
- 오타 수정, 변수명 변경 같은 일회성 작업은 묻지 않는다.
- practice를 여러 줄로 늘리지 않는다 — 한 줄 원칙.
