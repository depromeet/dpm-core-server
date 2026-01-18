# Research: @Controller Endpoints in Springdoc OpenAPI

## Problem Statement

**Current Issue**: `@Controller` endpoints (like `MemberLoginController`) are not visible in Swagger UI, but `@RestController` endpoints are visible.

**Challenge**:
- `@Controller` is needed for OAuth2 redirect endpoints (returns String view names like "redirect:/oauth2/authorization/kakao")
- `@RestController` would show in Swagger but breaks redirect functionality (renders redirect as plain text)

## Root Cause Analysis

### Why @Controller is Invisible to Springdoc

Springdoc OpenAPI only scans for endpoints that:
1. Return response bodies (JSON/XML)
2. Use `@RestController` or `@ResponseBody` annotations
3. Are explicitly configured through `OpenApiCustomizer`

`@Controller` without `@ResponseBody` is used for traditional MVC views (Thymeleaf, JSP, etc.) and returns view names that Spring renders. Springdoc excludes these by default because they don't return JSON responses.

### Current Implementation

**File**: `MemberLoginController.kt:17-18`
```kotlin
@Tag(name = "Member-Login", description = "Member Login API")
@Controller
class MemberLoginController(
```

The `@Tag` annotation is present, but since the class uses `@Controller` (not `@RestController`), Springdoc ignores these endpoints.

## Solutions Research

### Solution 1: Use @Controller + @ResponseBody Mixed (RECOMMENDED)

Add `@ResponseBody` only to the POST endpoint that returns JSON, while keeping GET endpoints as pure redirect handlers:

```kotlin
@Tag(name = "Member-Login", description = "Member Login API")
@Controller
class MemberLoginController(
    // ... dependencies
) {
    @GetMapping("/login/kakao")
    fun login(...): String {  // Returns redirect - works as-is
        return "redirect:/oauth2/authorization/kakao"
    }

    @GetMapping("/login/apple")
    fun appleLogin(...): String {  // Returns redirect - works as-is
        return "redirect:/oauth2/authorization/apple"
    }

    // This endpoint will now show in Swagger!
    @PostMapping("/v1/auth/login/apple")
    @ResponseBody  // ← ADD THIS
    fun appleLoginV1(@RequestBody body: AppleLoginRequest): AuthTokenResponse {
        // ...
    }
}
```

**Pros**:
- Minimal code change
- POST endpoint shows in Swagger
- Redirects still work correctly
- No need for manual OpenAPI configuration

**Cons**:
- GET redirect endpoints still won't show in Swagger (acceptable since they're just redirects)

### Solution 2: OpenApiCustomizer (Full Control)

Manually add all endpoints using `OpenApiCustomizer` bean:

**File**: `SwaggerConfig.kt`

```kotlin
@Bean
fun memberLoginControllerOpenApiCustomiser(): OpenApiCustomiser = OpenApiCustomiser { openApi: OpenAPI ->
    val paths = openApi.paths ?: io.swagger.v3.oas.models.Paths()

    // GET /login/kakao
    val kakaoLogin = PathItem()
    val getKakaoOp = io.swagger.v3.oas.models.operation.Operation()
    getKakaoOp.summary = "Kakao OAuth2 Login Redirect"
    getKakaoOp.description = "Redirects to Kakao OAuth2 authorization page"
    getKakaoOp.operationId = "kakaoLogin"
    getKakaoOp.tags = setOf("Member-Login")
    getKakaoOp.responses = mapOf(
        "302" to io.swagger.v3.oas.models.response.Response().description("Found - Redirect to Kakao"),
        "400" to io.swagger.v3.oas.models.response.Response().description("Bad Request")
    )
    kakaoLogin.get = getKakaoOp

    // GET /login/apple
    val appleLogin = PathItem()
    val getAppleOp = io.swagger.v3.oas.models.operation.Operation()
    getAppleOp.summary = "Apple OAuth2 Login Redirect"
    getAppleOp.description = "Redirects to Apple OAuth2 authorization page"
    getAppleOp.operationId = "appleLogin"
    getAppleOp.tags = setOf("Member-Login")
    getAppleOp.responses = mapOf(
        "302" to io.swagger.v3.oas.models.response.Response().description("Found - Redirect to Apple"),
        "400" to io.swagger.v3.oas.models.response.Response().description("Bad Request")
    )
    appleLogin.get = getAppleOp

    // POST /v1/auth/login/apple
    val appleLoginV1 = PathItem()
    val postAppleOp = io.swagger.v3.oas.models.operation.Operation()
    postAppleOp.summary = "Apple OAuth2 Login V1"
    postAppleOp.description = "Login with Apple authorization code"
    postAppleOp.operationId = "appleLoginV1"
    postAppleOp.tags = setOf("Member-Login")

    // Request body
    val reqBody = io.swagger.v3.oas.models.parameters.RequestBody()
    val content = io.swagger.v3.oas.models.media.Content()
    val mediaType = io.swagger.v3.oas.models.media.MediaType()
    val schema = io.swagger.v3.oas.models.media.Schema<Any>()
    schema.type = "object"
    val props = mapOf("authorizationCode" to io.swagger.v3.oas.models.media.Schema<String>().type("string"))
    schema.properties = props
    schema.required = listOf("authorizationCode")
    mediaType.schema = schema
    content.addMediaType("application/json", mediaType)
    reqBody.content = content
    postAppleOp.requestBody = reqBody

    postAppleOp.responses = mapOf(
        "200" to io.swagger.v3.oas.models.response.Response().description("OK - Login successful"),
        "401" to io.swagger.v3.oas.models.response.Response().description("Unauthorized"),
        "500" to io.swagger.v3.oas.models.response.Response().description("Internal Server Error")
    )
    appleLoginV1.post = postAppleOp

    paths.addPathItem("/login/kakao", kakaoLogin)
    paths.addPathItem("/login/apple", appleLogin)
    paths.addPathItem("/v1/auth/login/apple", appleLoginV1)

    openApi.paths(paths)
}
```

**Pros**:
- Full control over documentation
- All endpoints show in Swagger
- Redirects still work correctly

**Cons**:
- Verbose code
- Need to manually sync with actual controller methods
- Maintenance burden

### Solution 3: Create Separate API Controller (Clean Architecture)

Create a separate `@RestController` for API documentation while keeping `@Controller` for web layer:

```kotlin
// MemberLoginController.kt - @Controller for redirects
@Controller
class MemberLoginController {
    @GetMapping("/login/kakao")
    fun login(...): String { /* redirect */ }

    @GetMapping("/login/apple")
    fun appleLogin(...): String { /* redirect */ }
}

// MemberLoginApiController.kt - @RestController for API docs
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Member-Login", description = "Member Login API")
class MemberLoginApiController(
    private val appleAuthService: AppleAuthService
) {
    @PostMapping("/login/apple")
    fun appleLogin(@RequestBody body: AppleLoginRequest): AuthTokenResponse {
        return appleAuthService.login(body.authorizationCode)
    }
}
```

**Pros**:
- Clean separation of concerns
- Automatic Swagger documentation
- Follows REST API best practices

**Cons**:
- Duplicate endpoint paths (can be confusing)
- More classes to maintain

## Recommended Approach

### Hybrid: Solution 1 + Minimal Solution 2

1. **Add `@ResponseBody` to the POST endpoint** - This makes the JSON-returning endpoint visible in Swagger automatically
2. **Add minimal OpenApiCustomizer for GET endpoints** - Document the redirect endpoints with minimal code

### Implementation

**Step 1**: Modify `MemberLoginController.kt`:

```kotlin
@Tag(name = "Member-Login", description = "Member Login API")
@Controller
class MemberLoginController(
    private val appleAuthService: AppleAuthService,
    private val securityProperties: SecurityProperties,
) {
    @GetMapping("/login/kakao")
    fun login(request: HttpServletRequest, response: HttpServletResponse): String {
        setCookie(request, response)
        return KAKAO_REDIRECT_URL
    }

    @GetMapping("/login/apple")
    fun appleLogin(request: HttpServletRequest, response: HttpServletResponse): String {
        setCookie(request, response)
        return APPLE_REDIRECT_URL
    }

    @PostMapping("/v1/auth/login/apple")
    @ResponseBody  // ← ADD THIS ANNOTATION
    @Operation(summary = "Apple OAuth2 Login V1", description = "Login with Apple authorization code")
    fun appleLoginV1(
        @RequestBody body: AppleLoginRequest,
        response: HttpServletResponse,
    ): AuthTokenResponse {
        val tokens = appleAuthService.login(body.authorizationCode)
        addTokenCookies(response, tokens)
        return tokens
    }
}
```

**Step 2**: Add minimal `OpenApiCustomizer` to `SwaggerConfig.kt`:

```kotlin
@Bean
fun loginRedirectEndpointsCustomizer(): OpenApiCustomiser = OpenApiCustomiser { openApi: OpenAPI ->
    val paths = openApi.paths ?: io.swagger.v3.oas.models.Paths()

    val redirectResponse = mapOf(
        "302" to io.swagger.v3.oas.models.response.Response()
            .description("Found - Redirects to OAuth2 authorization page")
    )

    // /login/kakao
    val kakaoPath = PathItem().apply {
        get(io.swagger.v3.oas.models.operation.Operation().apply {
            summary = "Kakao OAuth2 Login"
            description = "Initiates Kakao OAuth2 authorization flow"
            operationId = "kakaoLogin"
            tags = setOf("Member-Login")
            responses = redirectResponse
        })
    }

    // /login/apple
    val applePath = PathItem().apply {
        get(io.swagger.v3.oas.models.operation.Operation().apply {
            summary = "Apple OAuth2 Login"
            description = "Initiates Apple OAuth2 authorization flow"
            operationId = "appleLogin"
            tags = setOf("Member-Login")
            responses = redirectResponse
        })
    }

    paths.addPathItem("/login/kakao", kakaoPath)
    paths.addPathItem("/login/apple", applePath)
    openApi.paths(paths)
}
```

## Key Findings

### Springdoc Behavior

1. **@RestController** = @Controller + @ResponseBody
2. **@Controller alone** → Springdoc assumes it returns view names, excludes from OpenAPI
3. **@ResponseBody** on method → Tells Springdoc "this method returns data, include in OpenAPI"

### Springdoc Properties

These properties can control what gets scanned:

```yaml
# application.yml
springdoc:
  show-actuator: true
  paths-to-match: /api/**,/v1/**  # Only scan certain paths
  packages-to-scan: core.application  # Only scan certain packages
  paths-to-exclude: /login/**  # Exclude paths
```

**Warning**: Excluding `/login/**` would hide the redirect endpoints even with customizer.

## Testing

After implementing, verify with:

```bash
# Start application
# Then check:
curl http://localhost:8080/v3/api-docs | jq '.paths | keys | map(select(contains("login")))'
```

Expected output:
```json
[
  "/login/kakao",
  "/login/apple",
  "/v1/auth/login/apple"
]
```

## References

- [Springdoc FAQ](https://springdoc.org/v1/faq.html)
- [SpringDoc endpoints don't show up - StackOverflow](https://stackoverflow.com/questions/71874996/springdoc-endpoints-doesnt-show-up)
- [SpringDoc GitHub](https://github.com/springdoc/springdoc-openapi)
- [SpringDoc OpenAPI Properties](https://springdoc.org/properties.html)
