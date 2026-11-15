---
name: openapi-spec
description: OpenAPI 스펙(`/v3/api-docs`)이 클라이언트 생성기에서 `unknown`이나 틀린 타입을 만들지 않도록 작성·점검하는 규칙. 새 컨트롤러·DTO 추가 시 / @ApiResponse 손댈 때 참고.
---

# OpenAPI 스펙 작성 규칙

클라이언트는 `openapi-typescript` 로 이 서버의 `/v3/api-docs` 에서 타입을 생성한다. 아래 규칙을 지키지 않으면 타입이 `unknown` 이거나 실제 응답과 달라진다.

전체 수정 이력은 PR [fix: OpenAPI 스펙 생성 문제 전반 정리](https://github.com/depromeet/dpm-core-server/pull/) 참고.

---

## 글로벌 설정 (한 번만)

`application/src/main/resources/application.yml` 에 아래가 있어야 한다. 삭제·변경 금지.

```yaml
springdoc:
  default-produces-media-type: application/json
  api-docs:
    version: openapi_3_0
```

- `default-produces-media-type` 이 없으면 `useReturnTypeSchema` 가 `*/*` 로 떨어져 `application/json` 키와 둘로 쪼개진다.
- `api-docs.version` 을 3.1 로 두면 `type: [..., "null"]` 문법이 나와 클라이언트 생성기가 호환되지 않는다. Kotlin nullable 도 `nullable: true` 로 안 떨어진다.

`application/src/main/kotlin/core/application/common/configuration/SwaggerConvertersConfig.kt` 가 `ResponseModelConverter` 를 **`ModelConverter` 빈** 으로 등록해야 한다. `OpenApiCustomizer` 로 늦게 추가하면 이미 만들어진 스키마에는 적용되지 않는다.

`OperationIdCustomizer` 빈은 value class 파라미터(`MemberId` 등)로 생기는 `-KYnTANU` 류 JVM 맹글링 접미사를 제거한다. 유지 필요.

---

## 응답 선언 패턴

### 성공 응답 (예시가 필요 없는 경우)

```kotlin
@ApiResponse(responseCode = "200", description = "...", useReturnTypeSchema = true)
```

반환 타입 `CustomResponse<SessionListResponse>` 에서 자동으로 스키마가 붙는다. 이 경우 `content` 는 쓰지 않는다.

### 성공 응답 (예시가 필요한 경우)

```kotlin
@ApiResponse(
    responseCode = "200",
    description = "...",
    useReturnTypeSchema = true,
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(ref = "#/components/schemas/CustomResponseSessionListResponse"),
            examples = [ExampleObject(name = "...", value = """...""")],
        ),
    ],
)
```

- `mediaType = "application/json"` 명시
- `schema = Schema(ref = "#/components/schemas/CustomResponse{DtoName}")` 을 반드시 명시
- `useReturnTypeSchema = true` 는 그대로 둔다 (중복이지만 설정상 안전)

### 에러 응답 (`CustomResponse<Void>`)

```kotlin
@ApiResponse(
    responseCode = "400",
    description = "...",
    content = [Content(schema = Schema(ref = "#/components/schemas/CustomResponseVoid"))],
)
```

- 짧게 설명만 붙이는 에러는 `description` 만 있어도 된다. springdoc 이 반환 타입에서 추론한다.
- `Schema(implementation = CustomResponse::class)` 를 **쓰지 않는다**. 제네릭 없는 raw 참조라 `data` 가 `unknown` 이 된다.

### 에러 응답에 `data` 가 있는 경우 (409 등)

```kotlin
@ApiResponse(
    responseCode = "409",
    description = "...",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = MemberDeletionBlockedResponse::class),
            examples = [ExampleObject(value = BLOCKED_EXAMPLE)],
        ),
    ],
)
```

- `data` 구조가 있는 에러는 문서화용 DTO 를 만들고 `Schema(implementation = ...)` 로 참조.
- 서버가 실제로 그 DTO 를 반환할 필요는 없다(문서 전용). 단, `data` 가 없는 케이스가 있으면 DTO 의 필드를 optional 로 둔다.

---

## 요청 DTO

### 배열 필드

Long · Int 등 기본형 요소 배열:

```kotlin
@ArraySchema(
    arraySchema = Schema(description = "...", nullable = true),
    schema = Schema(type = "integer", format = "int64", example = "12"),
)
val imageIds: List<Long?>? = null
```

`type = "integer"` 가 OpenAPI 3.0 에서 다시 동작한다. `types = ["integer"]` 는 3.1 전용이며 쓰지 않는다.

### nullable 필드

Kotlin `T?` 는 `nullable: true` 로 자동 변환된다. 명시가 필요하면:

```kotlin
@field:Schema(description = "...", nullable = true)
val part: String?,
```

`types = [..., "null"]` 은 사용 금지.

### enum 필드

Kotlin `enum class` 는 `enum: [VAL1, VAL2, ...]` 로 떨어진다. Jackson 이 `name()` 으로 직렬화하므로 추가 작업 불필요.

---

## 금지 패턴

| 패턴 | 대신 | 이유 |
|---|---|---|
| `Schema(implementation = CustomResponse::class)` | `Schema(ref = "#/components/schemas/CustomResponseVoid")` | raw 제네릭 참조는 `data: unknown` |
| `Schema(type = "integer")` on `@ArraySchema.arraySchema` | `@ArraySchema.schema` 에 `type = "integer"` | 3.1 에서 무시됨 |
| `types = ["null", "string"]` | `nullable = true` 또는 Kotlin `T?` | 3.0 모드에선 반영 안 됨 |
| `@Schema(types = ["string"])` | `@Schema(type = "string")` | 3.0 모드 기준 |
| `OpenApiCustomizer { ModelConverters.addConverter(...) }` | `@Bean fun x(): ModelConverter = ...` | 스키마 생성 이후에 실행되어 적용 안 됨 |

---

## 회귀 테스트

`MemberManagementContractControllerTest` 가 `/v3/api-docs` 를 MockMvc 로 만들어 세 가지를 검증한다.

1. `CustomResponse*` 스키마 모두 `status.type == "string"`, `enum` 없음
2. 모든 응답 `content` 의 schema 가 비어있지 않음
3. `operationId` 에 `-[A-Za-z0-9_]+$` 접미사가 남아있지 않음

새 컨트롤러·응답을 추가했으면 이 테스트에 경로를 늘려 같은 검증을 추가한다.

---

## 손으로 확인하는 방법

```bash
curl -s http://localhost:8080/v3/api-docs -o /tmp/api-docs.json
python3 - <<'EOF'
import json
d = json.load(open('/tmp/api-docs.json'))
S = d['components']['schemas']
def empty(s): return not any(k in s for k in ('type', 'types', '$ref', 'properties', 'items', 'oneOf', 'anyOf', 'allOf', 'enum'))
no_schema = [f"{m.upper()} {p} {st} {mt}"
             for p, ops in d['paths'].items() for m, op in ops.items()
             for st, r in op.get('responses', {}).items()
             for mt, c in (r.get('content') or {}).items() if empty(c.get('schema') or {})]
status_enum = [n for n, s in S.items()
               if n.startswith('CustomResponse') and 'enum' in s.get('properties', {}).get('status', {})]
untyped_items = [f"{n}.{k}" for n, s in S.items() for k, v in s.get('properties', {}).items()
                 if 'items' in v and empty(v['items'])]
print('1. schema 없는 응답 content:', len(no_schema), *no_schema, sep='\n  ')
print('2. status enum 스키마:', len(status_enum))
print('3. 타입 없는 배열 요소:', untyped_items)
EOF
```

필수 항목이 모두 `0` 이면 OK.

---

## 새 API 작성 체크리스트

- [ ] 반환 타입이 `CustomResponse<T>` 다
- [ ] 성공 응답에 `useReturnTypeSchema = true` 가 있고, `content` 를 쓸 땐 `schema = Schema(ref = ...)` 를 명시한다
- [ ] 에러 응답의 `content.schema` 는 `Schema(ref = "#/components/schemas/CustomResponseVoid")` 또는 전용 DTO 참조다
- [ ] `Schema(implementation = CustomResponse::class)` 가 없다
- [ ] `imageIds` 같은 primitive 배열은 `@ArraySchema.schema` 에 `type` 과 `format` 을 명시했다
- [ ] 로컬에서 `/v3/api-docs` 또는 `MemberManagementContractControllerTest` 로 검증했다
