---
name: feature
description: 요구사항을 받아 검토→구현→리뷰→PR까지 멀티 에이전트로 자동 진행하는 피처 개발 워크플로우.
---

# /feature — 피처 개발 워크플로우

세 에이전트가 순서대로 실행된다:

```
feature-planner     (검토)  → 요구사항 분석, 질문, 계획 수립
feature-implementer (구현)  → 헥사고날 준수, 테스트, DB 마이그레이션
springboot-kotlin-reviewer (리뷰) → 아키텍처·다기수·도메인·보안 검토
```

---

## 실행 흐름

### Phase 1 — feature-planner 실행

`feature-planner` 에이전트를 호출한다.

에이전트가 하는 일:
- Figma URL이 있으면 Figma MCP로 UI 분석 → 요구사항 추출
- 도메인·영향 레이어·다기수 여부·DB 변경 필요 여부 스스로 추론
- 추론 불가한 것만 사용자에게 질문 (worktree 여부, PR 여부 포함)
- 구현 계획 출력 (파일 목록, API 스펙, 테스트 케이스, 다기수 시나리오, DB 변경)

사용자가 계획을 승인(`y`)하면 Phase 2로 진행.  
수정 요청이 있으면 계획을 반영한 뒤 재승인.

---

### Phase 2 — feature-implementer 실행

승인된 계획을 `feature-implementer` 에이전트에 전달한다.

에이전트가 하는 일:
- worktree/브랜치 생성 (계획에 따라)
- domain → persistence → application 순으로 구현
- 단위 테스트 (Fake 포트 패턴)
- 계약 테스트 (신규 API)
- 통합 테스트 (복잡한 쿼리, 다기수 시나리오)
- DB 마이그레이션 SQL 작성 (변경 있을 때)
- ktlint 자동 포맷 (PostToolUse 훅)

완료 후 구현 파일 목록을 요약해서 출력.

---

### Phase 3 — springboot-kotlin-reviewer 실행

구현 파일 목록을 `springboot-kotlin-reviewer` 에이전트에 전달한다.

에이전트가 검토하는 항목:
- 헥사고날 아키텍처 위반 (레이어 의존 방향, Port 우회)
- 다기수 시나리오 누락 (activeCohortId 필터, 이전 기수 접근 차단)
- 도메인 상태 전이 오류 (PENDING → EXCUSED_ABSENT 직접 전환 등)
- Kotlin 관용구 (`!!`, `var`, sealed class else 분기)
- 트랜잭션·동시성 (전파 범위, 낙관적/비관적 락)
- 보안 (`@PreAuthorize` 누락, SQL Injection)
- 테스트 커버리지 누락

**결과에 따른 분기:**

🔴 BLOCK 이슈 있음
→ feature-implementer에게 이슈 목록 전달해서 재작업
→ 재작업 완료 후 springboot-kotlin-reviewer 재실행

🟡 WARN 이하만 있음
→ WARN 목록을 사용자에게 보여주고 계속 진행 여부 확인
→ 확인 후 Phase 4로 진행

---

### Phase 4 — 커밋 · PR 생성

PR까지 올리기로 한 경우:

```bash
git add <구현 파일들>
git commit -m "<type>: <설명>"
git push -u origin HEAD

gh pr create \
  --base develop \
  --assignee "@me" \
  --reviewer hwistlezz \
  --reviewer cowboysj \
  --label "<커밋 타입에 맞는 label>" \
  --title "<type>: <설명>" \
  --body "..."
```

PR 본문: `.claude/skills/git-workflow.md` 의 PR 본문 구조 사용.

로컬 커밋까지만인 경우: 커밋 후 PR 생성 생략.

---

## 참조 스킬

| 단계 | 참조 |
|---|---|
| 아키텍처 판단 | `.claude/skills/hexagonal-arch.md` |
| 도메인 상태값 | `.claude/skills/domain-model.md` |
| API 작성 | `.claude/skills/api-design.md` |
| 다기수 시나리오 | `.claude/skills/multi-cohort-scenarios.md` |
| 테스트 패턴 | `.claude/skills/testing-patterns.md` |
| DB 마이그레이션 | `.claude/skills/db-migration.md` |
| Worktree | `.claude/skills/git-worktree.md` |
| 커밋·PR | `.claude/skills/git-workflow.md` |
