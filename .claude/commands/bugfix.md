---
name: bugfix
description: 버그 증상을 받아 원인 파악→최소 수정→리뷰→PR까지 빠르게 진행하는 버그 수정 워크플로우.
---

# /bugfix — 버그 수정 워크플로우

`/feature`보다 빠르다. 계획 단계를 최소화하고 원인 특정 → 최소 수정에 집중한다.

```
bugfix-investigator  (원인 파악)  → 코드 탐색, 원인 파일·라인 특정, 수정 계획
                                     ↓ 승인
                     (수정)       → 최소 변경 + 회귀 테스트
                                     ↓
springboot-kotlin-reviewer (리뷰) → 수정 범위·사이드 이펙트 확인
                                     ↓
                     (PR)         → fix/ 브랜치, label 🐞 Bugfix
```

---

## Phase 1 — bugfix-investigator 실행

`bugfix-investigator` 에이전트를 호출한다.

입력 형태:
- 증상 설명 텍스트
- 스택 트레이스 or 에러 로그 (있으면 포함)
- 에러코드 (있으면 포함, 예: `ATTENDANCE-403-01`)
- 재현 조건 (알고 있으면)

에이전트가 하는 일:
- 질문 없이 코드 탐색 먼저
- 원인 파일·라인 특정
- 최소 수정 계획 제시

사용자가 `y` 하면 Phase 2 진행.  
"다른 원인인 것 같아요" 등 이의가 있으면 재탐색.

---

## Phase 2 — 수정

**원칙: 버그 재현 범위만 수정한다. 리팩터링·기능 추가 금지.**

```
1. 브랜치 생성
   git checkout -b fix/<짧은-설명> develop

2. 최소 변경
   - 원인으로 특정된 파일만 수정
   - 연쇄 수정이 필요하면 investigator에게 확인 후 진행

3. 회귀 테스트 작성
   - 버그를 재현하는 테스트 먼저 (Red)
   - 수정 후 통과 확인 (Green)
   - 다기수 버그라면 해당 페르소나 케이스 포함

4. 커밋
   git commit -m "fix: <한 줄 설명>"
```

**회귀 테스트 패턴:**

```kotlin
// 버그 재현 케이스 — 수정 전 실패, 수정 후 통과
@Test
fun `P-MULTI 멤버는 활성 기수 출석만 조회된다`() {
    // 버그: activeCohortId 필터 없어서 전체 기수 반환됐던 케이스
    val result = service.getAttendances(multiCohortMemberId, activeCohortId)
    assertThat(result).allMatch { it.cohortId == activeCohortId }
}
```

---

## Phase 3 — springboot-kotlin-reviewer 실행

수정 파일 목록을 `springboot-kotlin-reviewer` 에이전트에 전달한다.

버그픽스 리뷰 집중 항목:
- 수정이 버그 범위를 벗어나지 않는가 (scope creep)
- 수정이 다른 케이스에서 사이드 이펙트를 일으키지 않는가
- 회귀 테스트가 버그를 실제로 재현하는가
- 동일한 버그가 다른 도메인에도 있지 않은가 (유사 패턴 확인)

**결과에 따른 분기:**

🔴 BLOCK → 수정 재작업 후 재검토  
🟡 WARN 이하 → 사용자 확인 후 PR 진행

---

## Phase 4 — PR 생성

```bash
gh pr create \
  --base develop \
  --assignee "@me" \
  --reviewer hwistlezz \
  --reviewer cowboysj \
  --label "🐞 Bugfix" \
  --title "fix: <설명>" \
  --body "..."
```

PR 본문 Summary에 반드시 포함:
- 버그 증상
- 원인 (파일·라인)
- 수정 내용
- 재현 조건 (있으면)

---

## /feature와 차이점

| | /bugfix | /feature |
|---|---|---|
| 질문 단계 | 최소 (원인 탐색 우선) | 도메인·영향 범위 확인 |
| 수정 범위 | 버그 재현 범위만 | 신규 기능 전체 |
| 테스트 | 회귀 테스트 중심 | 단위·계약·통합 전체 |
| 브랜치 prefix | `fix/` | `feat/` |
| label | 🐞 Bugfix | ✨ Feature |
