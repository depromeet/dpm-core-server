---
name: api-design
description: dpm-core-server API 작성 패턴. CustomResponse 래핑, Controller/Api 파일 분리, 권한 어노테이션, 에러코드 체계, URL 버전 정책.
---

# API 작성 패턴

## 응답 래핑

모든 성공 응답은 `CustomResponse<T>` 로 래핑한다.

```kotlin
// Controller 반환 타입
fun getAttendances(...): CustomResponse<MemberAttendancesResponse> =
    CustomResponse.ok(service.getMemberAttendances(...))

// 생성 응답
fun createSession(...): CustomResponse<SessionResponse> =
    CustomResponse.created(service.createSession(...))
```

`CustomResponse.ok(data)` → 200 + data, `CustomResponse.created(data)` → 201 + data
No-content API는 **`CustomResponse.noContent()`** 사용 (data 필드 자체가 없음).

```kotlin
// 본문 반환 없음
fun deleteSession(...): CustomResponse<Nothing> =
    CustomResponse.noContent().also { service.deleteSession(...) }
```

> ⚠️ `CustomResponse.ok(Unit)`은 쓰지 않는다. `data: {}` 가 응답에 포함되어 No-content 의미가 아니다.

---

## Controller / Api 파일 분리

```
AttendanceQueryApi.kt      ← Swagger 선언만 (인터페이스)
AttendanceQueryController.kt ← @RestController 구현체
```

**Api 인터페이스** — Swagger 어노테이션, `@Tag`, `@Operation`, `@ApiResponses` 담당.
**Controller** — `@RestController`, `@PreAuthorize`, `@GetMapping` 등 HTTP 매핑 담당. Api 인터페이스를 `implements`.

---

## 인증 · 권한

```kotlin
@RestController
class AttendanceQueryController : AttendanceQueryApi {

    @PreAuthorize("hasAuthority('create:attendance')")
    @GetMapping("/v3/members/attendances")
    override fun getMemberAttendances(
        @CurrentMemberId memberId: MemberId,
        ...
    ): CustomResponse<MemberAttendancesResponse> { ... }
}
```

- `@CurrentMemberId` — SecurityContext에서 memberId 주입.
- `hasAuthority('...')` 권한 목록: `create:attendance`, `read:attendance`, `update:attendance`, `create:session`, `read:session`, `update:member`, `read:member` 등.
- 로그인 여부만 확인: `isAuthenticated()`.
- 권한 판정은 `AGENTS.md` 도메인 규칙 / `.agents/skills/testing-patterns/SKILL.md` 다기수 매트릭스 참조.

---

## 에러코드 체계

형식: `도메인-HTTP상태코드-순번`

```kotlin
enum class AttendanceExceptionCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ExceptionCode {
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ATTENDANCE-404-01", "출석 정보를 찾을 수 없습니다."),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "ATTENDANCE-409-01", "이미 출석이 완료되었습니다."),
    ;
}
```

전역 공통 코드: `GlobalExceptionCode` (`GLOBAL-200-01`, `GLOBAL-400-01`, `GLOBAL-403-01` 등).

---

## URL 버전 정책

- `/v3/` — 신규 표준. 새 엔드포인트는 전부 `/v3/`로 시작.
- `/v1/` · `/v2/` — application 모듈에 현재 운영 중 (출석·세션 피드백·사유서·공지 등). 레거시 아님.
- **버전 올리기 규칙**: DTO·파라미터·응답 구조가 바뀌면 버전을 올린다(`/v2/` → `/v3/`). 기존 버전은 유지한 채 배포 → 프론트 작업 완료 후 기존 버전 삭제.
- 같은 도메인에 v1·v2·v3 혼재 가능 (버전 전환 중).

---

## Swagger `@Operation` 필수 항목

```kotlin
@Operation(
    summary = "세션별 출석 명단 조회 (운영진)",
    description = "현재 활성 기수 세션의 출석 명단을 전부 조회합니다. QA 계정(@dpm.test) 제외.",
)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "200", description = "조회 성공", content = [...]),
        ApiResponse(responseCode = "403", description = "권한 없음"),
        ApiResponse(responseCode = "404", description = "세션 없음"),
    ]
)
```

---

## 페이지네이션 없는 전체 조회

이 프로젝트는 프론트 필터링 방식을 사용한다. 필터 없이 전체를 반환하고 프론트에서 필터링.

```kotlin
// 페이지네이션 미사용 예시
@GetMapping("/v3/sessions/{sessionId}/attendances")
fun getSessionRoster(
    @PathVariable sessionId: SessionId,
    @CurrentMemberId memberId: MemberId,
): CustomResponse<SessionRosterResponse> = ...
// → SessionRosterResponse 안에 members: List<MemberAttendanceDto> 전부 반환
```

---

## QA 계정 필터링

집계·목록 쿼리에서 `signupEmail LIKE '%@dpm.test'` 계정을 제외한다.
로그인·마이페이지 등 개인 쿼리는 제외하지 않는다.

---

## 주요 VO 타입

```kotlin
// PathVariable / RequestParam에 직접 사용
SessionId   // value: Long
MemberId    // value: Long
CohortId    // value: Long
```

Converter 등록 필요 없음 — 프레임워크 레벨에서 처리됨.
