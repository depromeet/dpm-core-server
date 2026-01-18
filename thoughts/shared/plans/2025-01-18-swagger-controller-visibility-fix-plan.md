---
date: 2025-01-18T11:50:00+09:00
planner: claude
based_on_research: 2025-01-18-controller-swagger-visibility-research.md
ticket: "Swagger @Controller Visibility"
branch: refactor/swagger-controller-fix
repository: dpm-core-server
status: ready_for_implementation
---

# Implementation Plan: Fix @Controller Swagger Visibility

**Date**: 2025-01-18 11:50:00 KST
**Planner**: claude
**Based on Research**: `2025-01-18-controller-swagger-visibility-research.md`
**Goal**: Make `@Controller` endpoints visible in Swagger/OpenAPI documentation while maintaining redirect functionality

## Problem Statement

### Current Situation

**File**: `MemberLoginController.kt:18-22`

```kotlin
@Tag(name = "Member-Login", description = "Member Login API")
@Controller  // ← Not @RestController
class MemberLoginController(
    // ...
)
```

**Issue**:
- `@Controller` is required for OAuth2 redirect endpoints (returns String view names)
- `@Controller` endpoints are NOT visible in Springdoc OpenAPI by default
- `@RestController` would make endpoints visible BUT breaks redirect functionality
- Springdoc only scans endpoints that return JSON/XML responses

### Current Endpoints

| Endpoint | Method | Returns | Visible in Swagger | Issue |
|----------|--------|---------|-------------------|-------|
| `/login/kakao` | GET | String ("redirect:/oauth2/authorization/kakao") | ❌ No | @Controller, returns view name |
| `/login/apple` | GET | String ("redirect:/oauth2/authorization/apple") | ❌ No | @Controller, returns view name |
| `/v1/auth/login/apple` | POST | AuthTokenResponse (JSON) | ✅ Yes | Has @ResponseBody |

## Root Cause Analysis

### Why @Controller is Invisible

Springdoc OpenAPI exclusion logic:
1. `@Controller` without `@ResponseBody` → Assumes MVC view rendering
2. Springdoc ONLY documents endpoints that return response bodies
3. String return values without `@ResponseBody` = view names, not JSON

### Why Previous OpenApiCustomizer Failed

**Attempted Approach** (failed with compilation errors):
```kotlin
@Bean
fun loginRedirectEndpointsCustomizer(): OpenApiCustomizer = OpenApiCustomizer { ... }
```

**Error**: `Unresolved reference: path`, `Unresolved reference: operation`, etc.

**Root Cause**: OpenAPI v3 library naming conflicts with Kotlin package structure:
- `io.swagger.v3.oas.models.path.PathItem` - conflict with Kotlin's `path` package
- `io.swagger.v3.oas.models.operation.Operation` - conflict resolution issue
- Fully qualified names don't resolve properly in Kotlin

## Solution Options

### Option 1: Add @ResponseBody to POST Endpoint Only ✅ (CURRENT STATUS)

**Implementation**: Already done
- POST endpoint has `@ResponseBody` → visible in Swagger
- GET redirect endpoints remain invisible → acceptable (they're simple redirects)

**Pros**:
- Minimal code change
- POST endpoint documented
- No compilation issues
- Redirects work correctly

**Cons**:
- GET endpoints not documented in Swagger
- Must use curl/browser to test redirects

**Status**: ✅ Implemented and working

---

### Option 2: Use RedirectView with @ResponseBody ⭐ RECOMMENDED

**Change GET endpoints to return `RedirectView` instead of String**:

```kotlin
@Controller
class MemberLoginController(
    private val appleAuthService: AppleAuthService,
    private val securityProperties: SecurityProperties,
) {
    @GetMapping("/login/kakao")
    @ResponseBody  // ← Add this
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): RedirectView {  // ← Change return type
        setCookie(request, response)
        return RedirectView("/oauth2/authorization/kakao")
    }

    @GetMapping("/login/apple")
    @ResponseBody  // ← Add this
    fun appleLogin(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): RedirectView {  // ← Change return type
        setCookie(request, response)
        return RedirectView("/oauth2/authorization/apple")
    }

    @PostMapping("/v1/auth/login/apple")
    @ResponseBody  // ← Already has this
    fun appleLoginV1(@RequestBody body: AppleLoginRequest): AuthTokenResponse {
        // ...
    }
}
```

**Pros**:
- All endpoints visible in Swagger
- Redirects still work correctly
- No manual OpenApiCustomizer needed
- Spring MVC handles `RedirectView` properly

**Cons**:
- Slightly different redirect mechanism (Spring's `RedirectView` vs String prefix)
- Need to verify cookie behavior still works

**Status**: ✅ RECOMMENDED SOLUTION

---

### Option 3: Use OpenApiCustomizer with Proper Imports ⚠️ COMPLEX

**Fix the import issues** by avoiding problematic packages:

```kotlin
// Use aliases to avoid naming conflicts
import io.swagger.v3.oas.models.path.PathItem as ApiPathItem
import io.swagger.v3.oas.models.operation.Operation as ApiOperation
import io.swagger.v3.oas.models.response.Response as ApiResponse

@Bean
fun loginRedirectEndpointsCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
    val paths = openApi.paths ?: io.swagger.v3.oas.models.Paths()

    val kakaoPath = ApiPathItem()
    val kakaoOp = ApiOperation()
    // ... set properties
    paths.addPathItem("/login/kakao", kakaoPath)

    val applePath = ApiPathItem()
    val appleOp = ApiOperation()
    // ... set properties
    paths.addPathItem("/login/apple", applePath)

    openApi.paths(paths)
}
```

**Pros**:
- Full control over documentation
- No changes to controller methods

**Cons**:
- Verbose code
- Maintenance burden (must keep in sync with controller)
- Import aliases are ugly
- Complex to set up request body schemas

**Status**: ⚠️ Consider if Option 2 doesn't work

---

### Option 4: Create Separate API Controller 🔄 CLEAN ARCHITECTURE

**Split web layer and API layer**:

```kotlin
// Web layer - handles redirects
@Controller
@RequestMapping("/login")
class LoginController {
    @GetMapping("/kakao")
    fun login(): String {
        return "redirect:/oauth2/authorization/kakao"
    }

    @GetMapping("/apple")
    fun appleLogin(): String {
        return "redirect:/oauth2/authorization/apple"
    }
}

// API layer - returns JSON, documented in Swagger
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Authentication APIs")
class AuthApiController(
    private val appleAuthService: AppleAuthService
) {
    @PostMapping("/login/apple")
    @Operation(summary = "Apple OAuth2 Login", description = "Login with Apple authorization code")
    fun appleLogin(@RequestBody body: AppleLoginRequest): AuthTokenResponse {
        return appleAuthService.login(body.authorizationCode)
    }
}
```

**Pros**:
- Clean separation of concerns
- Automatic Swagger documentation
- Follows REST API best practices
- No manual configuration needed

**Cons**:
- Duplicate cookie-setting logic
- Two controllers instead of one
- More files to maintain

**Status**: 🔄 Good for long-term architecture

---

## Implementation Plan: Option 2 (Recommended)

### Phase 1: Modify GET Endpoints to Use RedirectView

**File**: `MemberLoginController.kt`

**Changes**:
1. Add `import org.springframework.web.servlet.view.RedirectView`
2. Add `@ResponseBody` to GET methods
3. Change return type from `String` to `RedirectView`
4. Return `RedirectView()` instead of redirect strings

**Code Changes**:
```kotlin
import org.springframework.web.servlet.view.RedirectView

@Tag(name = "Member-Login", description = "Member Login API")
@Controller
class MemberLoginController(
    private val appleAuthService: AppleAuthService,
    private val securityProperties: SecurityProperties,
) {
    companion object {
        // Remove these constants, no longer needed
        // private const val KAKAO_REDIRECT_URL = "redirect:/oauth2/authorization/kakao"
        // private const val APPLE_REDIRECT_URL = "redirect:/oauth2/authorization/apple"
        private const val REQUEST_DOMAIN = "REQUEST_DOMAIN"
        // ...
    }

    @GetMapping("/login/kakao")
    @ResponseBody
    @Operation(
        summary = "Kakao OAuth2 Login Redirect",
        description = "Initiates Kakao OAuth2 authorization flow. Sets REQUEST_DOMAIN cookie and redirects to Kakao."
    )
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): RedirectView {
        setCookie(request, response)
        return RedirectView("/oauth2/authorization/kakao")
    }

    @GetMapping("/login/apple")
    @ResponseBody
    @Operation(
        summary = "Apple OAuth2 Login Redirect",
        description = "Initiates Apple OAuth2 authorization flow. Sets REQUEST_DOMAIN cookie and redirects to Apple."
    )
    fun appleLogin(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): RedirectView {
        setCookie(request, response)
        return RedirectView("/oauth2/authorization/apple")
    }

    @PostMapping("/v1/auth/login/apple")
    @ResponseBody
    @Operation(
        summary = "Apple OAuth2 Login V1",
        description = "Login with Apple authorization code to receive JWT tokens"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Login successful - returns JWT tokens"),
            ApiResponse(responseCode = "401", description = "Invalid authorization code"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
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

### Phase 2: Verify Compilation

**Command**:
```bash
./gradlew :application:compileKotlin
```

**Expected Result**: `BUILD SUCCESSFUL`

### Phase 3: Test Redirect Functionality

**Test 1**: Kakao Login Redirect
```bash
curl -i http://localhost:8080/login/kakao \
  -H "Origin: http://localhost:3000" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"
```

**Expected Result**:
```
HTTP/1.1 302
Location: /oauth2/authorization/kakao
Set-Cookie: REQUEST_DOMAIN=localhost; ...
```

**Test 2**: Apple Login Redirect
```bash
curl -i http://localhost:8080/login/apple \
  -H "Origin: http://localhost:3000" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"
```

**Expected Result**:
```
HTTP/1.1 302
Location: /oauth2/authorization/apple
Set-Cookie: REQUEST_DOMAIN=localhost; ...
```

**Test 3**: Verify OAuth2 Flow (Full Integration)
```bash
# Start application (use IntelliJ, not bootRun)
# Then test in browser or use full OAuth2 flow
```

### Phase 4: Verify Swagger Documentation

**Access Swagger UI**:
```bash
# In browser
open http://localhost:8080/swagger-ui/index.html

# Or check OpenAPI JSON
curl http://localhost:8080/v3/api-docs | jq '.paths | keys | map(select(contains("login")))'
```

**Expected Results**:
```json
[
  "/login/kakao",
  "/login/apple",
  "/v1/auth/login/apple"
]
```

**Verify in Swagger UI**:
1. Open Swagger UI
2. Look for "Member-Login" tag
3. All three endpoints should be visible
4. Each should have proper documentation

### Phase 5: Update SwaggerConfig (Cleanup)

**File**: `SwaggerConfig.kt`

**Action**: Remove or update the comment about @Controller visibility:

```kotlin
// Note: All MemberLoginController endpoints are now visible in Swagger.
// GET endpoints use RedirectView with @ResponseBody for proper OAuth2 redirects.
// POST endpoint returns JSON with @ResponseBody.
// No manual OpenApiCustomizer needed - Springdoc scans them automatically.
```

---

## Alternative Implementation: Option 4 (If Option 2 Fails)

### Create Separate API Controller

**New File**: `AuthApiController.kt`

```kotlin
package core.application.member.presentation.controller

import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.AuthTokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Authentication", description = "Authentication and Authorization APIs")
@RestController
@RequestMapping("/v1/auth")
class AuthApiController(
    private val appleAuthService: AppleAuthService,
) {
    @PostMapping("/login/apple")
    @Operation(
        summary = "Apple OAuth2 Login",
        description = "Login with Apple authorization code to receive JWT tokens"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Login successful - returns JWT tokens"),
            ApiResponse(responseCode = "401", description = "Invalid authorization code"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun appleLogin(
        @RequestBody body: AppleLoginRequest,
        response: HttpServletResponse,
    ): AuthTokenResponse {
        val tokens = appleAuthService.login(body.authorizationCode)

        // Add cookies (moved from MemberLoginController)
        val accessTokenCookie = createCookie("accessToken", tokens.accessToken, 60 * 60 * 24)
        val refreshTokenCookie = createCookie("refreshToken", tokens.refreshToken, 60 * 60 * 24 * 30)
        response.addCookie(accessTokenCookie)
        response.addCookie(refreshTokenCookie)

        return tokens
    }

    private fun createCookie(name: String, value: String, maxAgeSeconds: Int): Cookie {
        return Cookie(name, value).apply {
            path = "/"
            maxAge = maxAgeSeconds
            isHttpOnly = true
        }
    }

    data class AppleLoginRequest(
        val authorizationCode: String,
    )
}
```

**Modify**: `MemberLoginController.kt`

```kotlin
@Controller
class MemberLoginController(
    // Remove appleAuthService, no longer needed here
    private val securityProperties: SecurityProperties,
) {
    @GetMapping("/login/kakao")
    fun login(...): String {
        setCookie(request, response)
        return "redirect:/oauth2/authorization/kakao"
    }

    @GetMapping("/login/apple")
    fun appleLogin(...): String {
        setCookie(request, response)
        return "redirect:/oauth2/authorization/apple"
    }

    // Remove POST endpoint - moved to AuthApiController

    // Keep helper methods
    private fun setCookie(...) { ... }
}
```

---

## Success Criteria

### Option 2 (RedirectView) Success Criteria:

- [ ] All three endpoints visible in Swagger UI
- [ ] `/login/kakao` → 302 redirect to `/oauth2/authorization/kakao`
- [ ] `/login/apple` → 302 redirect to `/oauth2/authorization/apple`
- [ ] `/v1/auth/login/apple` → returns JWT tokens
- [ ] REQUEST_DOMAIN cookie set correctly
- [ ] Full OAuth2 flow works (Kakao and Apple)
- [ ] No compilation errors
- [ ] No regressions in existing functionality

### Option 4 (Split Controllers) Success Criteria:

- [ ] GET redirects work correctly
- [ ] POST endpoint documented in Swagger
- [ ] Cookie setting works in both controllers
- [ ] No code duplication (or minimal)
- [ ] Clean separation of concerns

---

## Rollback Plan

If Option 2 breaks redirect functionality:

1. **Revert** `MemberLoginController.kt` to use String returns
2. **Keep** `@ResponseBody` on POST endpoint only
3. **Accept** that GET endpoints won't be in Swagger
4. **Document** endpoints using alternative methods (README, API docs, etc.)

Rollback command:
```bash
git checkout HEAD -- application/src/main/kotlin/core/application/member/presentation/controller/MemberLoginController.kt
```

---

## Estimated Time

- **Option 2 Implementation**: 30 minutes
  - Code changes: 15 minutes
  - Testing: 15 minutes

- **Option 4 Implementation**: 45 minutes
  - Create new controller: 20 minutes
  - Move/refactor code: 15 minutes
  - Testing: 10 minutes

---

## Next Steps

1. **Implement Option 2** (RedirectView approach)
2. **Test thoroughly** with curl and browser
3. **Verify Swagger documentation**
4. **If issues arise**, consider Option 4

---

## References

- Research Document: `2025-01-18-controller-swagger-visibility-research.md`
- SpringDoc Documentation: [https://springdoc.org/](https://springdoc.org/)
- Spring Framework: RedirectView javadoc
- StackOverflow: [SpringDoc endpoints don't show up](https://stackoverflow.com/questions/71874996/springdoc-endpoints-doesnt-show-up)
