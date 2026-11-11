---
name: git-worktree
description: 복수 작업을 병렬로 진행할 때 git worktree 생성·포트 조정·정리 방법.
---

# Git Worktree

> **이 스킬을 쓰는 경우:** 현재 브랜치를 유지하면서 다른 브랜치를 동시에 개발할 때.  
> 복수의 서버를 동시에 띄우면 포트 충돌이 발생하므로 포트를 조정해야 한다.

---

## 워크트리 생성

```bash
# 새 브랜치로 워크트리 생성
git worktree add <경로> -b <브랜치명>

# 예
git worktree add ~/worktrees/feat-session-feedback -b feat/session-feedback-form
```

경로 관례: `~/worktrees/<브랜치-짧은이름>`

기존 브랜치를 체크아웃만 할 때:
```bash
git worktree add <경로> <기존-브랜치명>
```

---

## 포트 충돌 방지

기본 포트는 `8080`. 워크트리에서 서버를 동시에 띄우면 충돌한다.

**워크트리별 포트 지정 — `.env` 파일로 오버라이드:**

워크트리 루트에 `.env` 파일을 만든다 (gitignore됨):
```properties
SERVER_PORT=8081
```

또는 실행 시 직접 지정:
```bash
./gradlew bootRun --args='--server.port=8081'
```

**포트 번호 관례:**

| 작업 | 포트 |
|---|---|
| 메인 워크트리 (기본) | 8080 |
| 첫 번째 워크트리 | 8081 |
| 두 번째 워크트리 | 8082 |

---

## 현재 워크트리 목록 확인

```bash
git worktree list
```

출력 예:
```
/Users/user/Desktop/dpm-core-server       abc1234 [chore/claude-harness]
/Users/user/worktrees/feat-session        def5678 [feat/session-feedback-form]
```

---

## 워크트리 정리

작업이 끝나고 PR 머지 후 삭제:

```bash
# 워크트리 제거
git worktree remove ~/worktrees/feat-session-feedback

# 브랜치도 삭제할 경우
git branch -d feat/session-feedback-form
```

오래된 워크트리 정리 (경로가 없어진 경우):
```bash
git worktree prune
```

---

## 주의 사항

- 같은 브랜치를 두 워크트리에서 동시에 체크아웃할 수 없다.
- `build/` 디렉토리는 워크트리마다 독립적으로 생성된다.
- 워크트리에서 jOOQ DSL이 없으면 `./gradlew :codegen:generateJooq` 를 별도 실행해야 한다.
- `.env` 파일은 gitignore됨 — 포트 설정이 커밋되지 않도록 주의.
