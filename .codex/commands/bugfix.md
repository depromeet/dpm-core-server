---
name: bugfix
description: 버그 증상을 받아 원인 파악→최소 수정→리뷰→PR까지 빠르게 진행하는 버그 수정 워크플로우.
---

# /bugfix — 버그 수정 워크플로우

`/feature`보다 빠르다. 계획 단계를 최소화하고 원인 특정 → 최소 수정에 집중한다.

```
bugfix-investigator  (원인 파악)  → 코드 탐색, 원인 파일·라인 특정, 수정 계획
                                     ↓ 승인
                     (수정)       → 회귀 테스트(Red) → 최소 변경(Green)
                                     ↓
springboot-kotlin-reviewer (리뷰) → 수정 범위·사이드 이펙트 확인
                                     ↓
                     (PR)         → 사용자가 "PR 올려줘" 명시 시에만, fix/ 브랜치, label 🐞 Bugfix
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

## Phase 2 — 수정 (TDD)

**원칙: 버그 재현 범위만 수정한다. 리팩터링·기능 추가 금지.**

```
1. 브랜치 생성
   git checkout -b fix/<짧은-설명> develop

2. 회귀 테스트 작성 (Red)
   - 버그를 재현하는 테스트 먼저 작성
   - ./gradlew :application:test --tests "<테스트명>" → 실패(Red) 확인
   - 다기수 버그라면 해당 페르소나 케이스 포함

3. 최소 변경 (Green)
   - 원인으로 특정된 파일만 수정
   - 연쇄 수정이 필요하면 investigator에게 확인 후 진행
   - ./gradlew :application:test --tests "<테스트명>" → 통과(Green) 확인

4. 전체 테스트 & 포맷
   - ./gradlew :application:test
   - ./gradlew ktlintCheck

5. 스테이징 & 커밋
   git add <수정 파일>
   git commit -m "fix: <한 줄 설명>"
```

**회귀 테스트 패턴:**

```kotlin
// 버그 재현 케이스 — 수정 전 실패(Red), 수정 후 통과(Green)
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

🔴 BLOCK → 재작업 흐름:
```
1. 리뷰 지적사항 반영 수정
2. ./gradlew :application:test → 통과 확인
3. git add <수정 파일>
4. git commit -m "fix: <재작업 내용>"  (amend 금지, 새 커밋)
5. git push (브랜치 이미 있으면 그냥 push, 없으면 -u)
6. 재검토 요청
```

🟡 WARN 이하 → 사용자 확인 후 커밋만 할지 PR까지 올릴지 묻는다.

---

## Phase 4 — 커밋·PR 생성 (명시적 지시 시에만)

사용자가 `"커밋해줘"` 하면 Phase 2의 커밋까지, `"PR 올려줘"` / `"PR까지"` 등 명시했을 때만 아래 실행.

**reviewer 결정:**
- 작성자가 `hwistlezz` → `--reviewer cowboysj` 만
- 작성자가 `cowboysj` → `--reviewer hwistlezz` 만
- 그 외 → `--reviewer hwistlezz --reviewer cowboysj`

```bash
# 작성자에 따라 reviewer 플래그 자동 계산
AUTHOR=$(git config user.name)
REVIEWER_FLAGS=""
[ "$AUTHOR" != "hwistlezz" ] && REVIEWER_FLAGS="$REVIEWER_FLAGS --reviewer hwistlezz"
[ "$AUTHOR" != "cowboysj" ]  && REVIEWER_FLAGS="$REVIEWER_FLAGS --reviewer cowboysj"

gh pr create \
  --base develop \
  --assignee "@me" \
  $REVIEWER_FLAGS \
  --label "🐞 Bugfix" \
  --title "fix: <설명>" \
  --body "..."
```

PR 본문 Summary에 반드시 포함:
- 버그 증상
- 원인 (파일·라인)
- 수정 내용
- 재현 조건 (있으면)
- 테스트 실행 결과 (`./gradlew :application:test` 통과 여부)

---

## /feature와 차이점

| | /bugfix | /feature |
|---|---|---|
| 질문 단계 | 최소 (원인 탐색 우선) | 도메인·영향 범위 확인 |
| 수정 범위 | 버그 재현 범위만 | 신규 기능 전체 |
| 테스트 | 회귀 테스트(Red → Green) | 단위·계약·통합 전체 |
| 브랜치 prefix | `fix/` | `feat/` |
| label | 🐞 Bugfix | ✨ Feature |
| 커밋·PR | 명시적 지시 시에만 | 명시적 지시 시에만 |
