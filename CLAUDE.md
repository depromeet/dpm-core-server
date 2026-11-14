@AGENTS.md

## 스킬 목록

| 스킬 | 용도 |
|---|---|
| `.claude/skills/hexagonal-arch.md` | 레이어 경계 규칙 |
| `.claude/skills/domain-model.md` | 도메인 용어·상태값 |
| `.claude/skills/api-design.md` | API 작성 패턴 |
| `.claude/skills/git-workflow.md` | 커밋·PR 규칙 |
| `.claude/skills/testing-patterns.md` | 테스트 작성 규칙 + 다기수 시나리오 |
| `.claude/skills/codebase-map.md` | 모듈·패키지·주요 클래스 위치 |
| `.claude/skills/dev-commands.md` | Gradle 커맨드, 테스트 실행, jOOQ 재생성 |
| `.claude/skills/db-migration.md` | db/pending/ SQL 작성 규칙, 파일명 형식, 배포 순서 |
| `.claude/skills/git-worktree.md` | 병렬 작업 시 worktree 생성·포트 조정·정리 |

## 코드 리뷰

Spring Boot + Kotlin PR 리뷰가 필요하면 `.claude/agents/springboot-kotlin-reviewer.md` 에이전트를 호출한다.
체크 항목: 헥사고날 위반, Kotlin 관용구, 트랜잭션·동시성, 도메인 상태 전이 오류, 보안.

## 학습 루프

교정 후 재발 방지할 규칙은 `/lesson` 으로 제안. 승인 없이 CLAUDE.md·skills에 직접 쓰지 말 것.
누적 교정: `.claude/memory/project.md` / 개인·시크릿: `CLAUDE.local.md` (gitignore됨)
