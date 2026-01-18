---
date: 2025-01-18T15:00:00+09:00
planner: claude
based_on_research: 2025-01-18-cookie-domain-validation-error-research.md
ticket: "Cookie CORS and OAuth Login Fix"
branch: refactor/cookie-cors-oauth-fix
repository: dpm-core-server
status: ready_for_implementation
---

# Implementation Plan: Cookie CORS, RefreshToken, and OAuth Login Fix

**Date**: 2025-01-18 15:00:00 KST
**Planner**: claude
**Based on Research**: `2025-01-18-cookie-domain-validation-error-research.md`
**Goal**: Fix cookie domain validation error, ensure CORS works correctly, and verify OAuth login flow saves refreshToken properly

---

## Problem Statement

### Current Issues

1. **Cookie Domain Validation Error** (Critical)
   - Error: `An invalid domain [.depromeet.shop] was specified for this cookie`
   - Impact: REQUEST_DOMAIN cookie not set → refreshToken not saved after OAuth2 login
   - Environment: Develop and Prod only (Local works fine)

2. **CORS and Cookie Configuration**
   - Cookies need to work across subdomains (core.depromeet.shop, api.depromeet.shop, admin.depromeet.shop)
   - SameSite=None requires Secure=true
   - Need to verify CORS allowed origins match cookie domain configuration

3. **OAuth Login Flow**
   - REQUEST_DOMAIN cookie set before OAuth2 redirect
   - RefreshToken cookie set after OAuth2 callback
   - Both cookies must be set successfully for login to work

### Error Context

```java
java.lang.IllegalArgumentException: An invalid domain [.depromeet.shop] was specified for this cookie
    at org.apache.tomcat.util.http.Rfc6265CookieProcessor.validateDomain(Rfc6265CookieProcessor.java:253)
```

**File**: `MemberLoginController.kt:96-113` (setCookie method)

---

## Root Cause Analysis

### Why It Failed After PR #232

**Before PR #232**:
- May have used different cookie configuration
- REQUEST_DOMAIN cookie might not have existed
- Cookies might have been set without domain attribute

**After PR #232**:
- `setCookie()` method introduced/modified
- Uses `securityProperties.cookie.domain` from environment variable
- `COOKIE_DOMAIN` in develop/prod has leading dot: `.depromeet.shop`
- Tomcat's `Rfc6265CookieProcessor` rejects domains starting with dots

### Configuration Analysis

**Current YAML Configuration** (`application.yml`):

```yaml
spring:
  security:
    cookie:
      domain: ${COOKIE_DOMAIN:localhost}
      secure: ${COOKIE_SECURE:true}
      http-only: ${COOKIE_HTTP_ONLY:false}

cors:
  allowed-origins:
    - https://core.depromeet.shop
    - https://admin.depromeet.shop
    - https://api.depromeet.shop
    - https://core.depromeet.com
    - https://admin.depromeet.com
    - https://api.depromeet.com
```

**Likely Environment Variables**:

| Environment | COOKIE_DOMAIN | Status |
|-------------|---------------|--------|
| LOCAL | `localhost` | ✅ Works |
| DEV | `.depromeet.shop` | ❌ Fails validation |
| PROD | `.depromeet.com` | ❌ Fails validation |

---

## Solution Architecture

### Cookie Flow

```
1. User clicks "Login with Apple" on core.depromeet.shop
   ↓
2. Frontend calls: GET https://api.depromeet.shop/login/apple
   Origin: https://core.depromeet.shop
   ↓
3. Backend sets REQUEST_DOMAIN cookie
   Set-Cookie: REQUEST_DOMAIN=core.depromeet.shop; Domain=depromeet.shop; Path=/; Secure; HttpOnly; SameSite=None
   ↓
4. Backend redirects to: https://appleid.apple.com/auth/authorize...
   ↓
5. Apple redirects back to: https://api.depromeet.shop/login/oauth2/code/apple
   ↓
6. Backend exchanges code for tokens, sets refreshToken cookie
   Set-Cookie: refreshToken=...; Domain=depromeet.shop; Path=/; Secure; HttpOnly; SameSite=None
   Set-Cookie: accessToken=...; Domain=depromeet.shop; Path=/; Secure; HttpOnly; SameSite=None
   ↓
7. Frontend on core.depromeet.shop can access refreshToken cookie
```

### Requirements

1. **Cookie Domain** must be set to `depromeet.shop` (no leading dot) for subdomain sharing
2. **SameSite=None** requires **Secure=true**
3. **CORS** must allow all frontend origins
4. **Credentials** must be included in CORS configuration

---

## Implementation Plan

### Phase 1: Environment Variable Configuration (DEV & PROD)

**Objective**: Fix cookie domain validation error

#### 1.1 Update Develop Environment

**File**: CI/CD Pipeline configuration (GitHub Actions, AWS CodeDeploy, etc.)

```yaml
# Example: .github/workflows/deploy-dev.yml
env:
  COOKIE_DOMAIN: "depromeet.shop"  # Remove leading dot
  COOKIE_SECURE: "true"
  COOKIE_HTTP_ONLY: "false"
```

**Before**:
```bash
export COOKIE_DOMAIN=.depromeet.shop  # WRONG
```

**After**:
```bash
export COOKIE_DOMAIN=depromeet.shop   # CORRECT
```

#### 1.2 Update Production Environment

**File**: CI/CD Pipeline configuration

```yaml
# Example: .github/workflows/deploy-prod.yml
env:
  COOKIE_DOMAIN: "depromeet.com"  # Remove leading dot
  COOKIE_SECURE: "true"
  COOKIE_HTTP_ONLY: "false"
```

**Before**:
```bash
export COOKIE_DOMAIN=.depromeet.com  # WRONG
```

**After**:
```bash
export COOKIE_DOMAIN=depromeet.com   # CORRECT
```

#### 1.3 Update Local Environment (Documentation)

**File**: `.env.example`

```bash
# Cookie Configuration
# IMPORTANT: Domain MUST NOT start with a dot (RFC6265)
COOKIE_DOMAIN=localhost
COOKIE_SECURE=false
COOKIE_HTTP_ONLY=true
```

---

### Phase 2: Verify and Update CORS Configuration

**Objective**: Ensure CORS is properly configured for cookie-based authentication

#### 2.1 Review Current CORS Configuration

**File**: Find CORS configuration file

Search for CORS configuration:
```bash
# Find files with @CrossOrigin or CorsConfiguration
rg -i "crossorigin" -l
rg -i "corsconfiguration" -l
```

#### 2.2 Verify CORS Allows Credentials

**Expected Configuration**:

```kotlin
@Configuration
class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "https://core.depromeet.shop",
                "https://admin.depromeet.shop",
                "https://local.depromeet.shop",
                "https://core.depromeet.com",
                "https://admin.depromeet.com"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowCredentials(true)  // ← IMPORTANT for cookies
            .allowedHeaders("*")
            .maxAge(3600)
    }
}
```

**Key Points**:
- `.allowCredentials(true)` must be set
- `.allowedOrigins()` must use specific origins (not `*` when using credentials)
- `Access-Control-Allow-Origin` must match request origin exactly

#### 2.3 Update CORS If Needed

If CORS doesn't allow credentials, update configuration:

```kotlin
@Bean
fun corsConfigurationSource(): CorsConfigurationSource {
    val configuration = CorsConfiguration()

    configuration.allowedOrigins = listOf(
        "https://core.depromeet.shop",
        "https://admin.depromeet.shop",
        "https://api.depromeet.shop",
        "https://local.depromeet.shop",
        "https://local-core.depromeet.shop:3010",
        "https://local-admin.depromeet.shop:3020",
        "https://core.depromeet.com",
        "https://admin.depromeet.com",
        "https://api.depromeet.com"
    )
    configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
    configuration.allowCredentials = true  // ← REQUIRED for cookies
    configuration.allowedHeaders = listOf("*")
    configuration.maxAge = 3600

    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", configuration)
    return source
}
```

---

### Phase 3: Verify Cookie Configuration

**Objective**: Ensure cookies are set with correct attributes

#### 3.1 Review Current Cookie Creation

**File**: `MemberLoginController.kt`

**Current Code** (lines 115-130):

```kotlin
private fun addTokenCookies(response: HttpServletResponse, tokens: AuthTokenResponse) {
    val accessTokenCookie = createCookie("accessToken", tokens.accessToken, 60 * 60 * 24) // 1 day
    val refreshTokenCookie = createCookie("refreshToken", tokens.refreshToken, 60 * 60 * 24 * 30) // 30 days

    response.addCookie(accessTokenCookie)
    response.addCookie(refreshTokenCookie)
}

private fun createCookie(name: String, value: String, maxAgeSeconds: Int): Cookie {
    return Cookie(name, value).apply {
        path = "/"
        maxAge = maxAgeSeconds
        isHttpOnly = true
        // secure = true // Enable in prod  ← TODO: This should be enabled!
    }
}
```

#### 3.2 Update Cookie Creation for Prod

**Issue**: `secure` is commented out for refreshToken, but should be enabled in prod

**Proposed Fix**:

```kotlin
private fun addTokenCookies(response: HttpServletResponse, tokens: AuthTokenResponse) {
    val accessTokenCookie = createCookie("accessToken", tokens.accessToken, 60 * 60 * 24)
    val refreshTokenCookie = createCookie("refreshToken", tokens.refreshToken, 60 * 60 * 24 * 30)

    response.addCookie(accessTokenCookie)
    response.addCookie(refreshTokenCookie)
}

private fun createCookie(name: String, value: String, maxAgeSeconds: Int): Cookie {
    return Cookie(name, value).apply {
        path = "/"

        // Use domain from configuration (without leading dot)
        domain = securityProperties.cookie.domain

        maxAge = maxAgeSeconds
        isHttpOnly = true
        secure = securityProperties.cookie.secure  // ← Use config value (true for dev/prod)

        // Set SameSite attribute
        // Note: SameSite=None requires Secure=true
        setAttribute("SameSite", "None")
    }
}
```

**Key Changes**:
1. Set `domain` from configuration (for subdomain sharing)
2. Set `secure` from configuration (true for dev/prod, false for local)
3. Always set `SameSite=None` (for cross-subdomain cookies)

---

### Phase 4: Update MemberLoginController

**Objective**: Fix cookie creation and domain handling

**File**: `application/src/main/kotlin/core/application/member/presentation/controller/MemberLoginController.kt`

#### 4.1 Update setCookie Method

**Current Code** (lines 96-113):

```kotlin
private fun setCookie(request: HttpServletRequest, response: HttpServletResponse) {
    try {
        val requestDomain =
            URI(request.getHeader(ORIGIN) ?: request.getHeader(REFERER) ?: request.requestURL.toString()).host

        Cookie(REQUEST_DOMAIN, requestDomain).apply {
            path = "/"
            domain = securityProperties.cookie.domain
            maxAge = 60
            isHttpOnly = true
            secure = true
            setAttribute("SameSite", "None")
            response.addCookie(this)
        }
    } catch (e: Exception) {
        logger.warn(e) { "Failed to set REQUEST_DOMAIN cookie : ${e.message}" }
    }
}
```

**Issue**: Extracted host might have leading dot, which is stored as cookie **value**, not domain attribute

**Analysis**:
- Cookie **name**: `REQUEST_DOMAIN`
- Cookie **value**: Extracted host (might be `.depromeet.shop`)
- Cookie **domain**: `securityProperties.cookie.domain` (now `depromeet.shop` without leading dot)

**Verdict**: Code is correct, just need to fix environment variable

**No code changes needed** - just update `COOKIE_DOMAIN` environment variable

#### 4.2 Update createCookie Method

**Current Code** (lines 123-130):

```kotlin
private fun createCookie(name: String, value: String, maxAgeSeconds: Int): Cookie {
    return Cookie(name, value).apply {
        path = "/"
        maxAge = maxAgeSeconds
        isHttpOnly = true
        // secure = true // Enable in prod
    }
}
```

**Proposed Change**:

```kotlin
private fun createCookie(name: String, value: String, maxAgeSeconds: Int): Cookie {
    return Cookie(name, value).apply {
        path = "/"
        domain = if (securityProperties.cookie.domain != "localhost") {
            securityProperties.cookie.domain
        } else {
            null  // Don't set domain for localhost
        }
        maxAge = maxAgeSeconds
        isHttpOnly = true
        secure = securityProperties.cookie.secure  // true for dev/prod, false for local
        setAttribute("SameSite", "None")
    }
}
```

**Changes**:
1. Set `domain` from configuration (except localhost)
2. Set `secure` from configuration
3. Always set `SameSite=None` for cross-subdomain cookies

---

### Phase 5: Testing and Verification

**Objective**: Thoroughly test the fix in all environments

#### 5.1 Local Testing

**Prerequisites**:
```bash
# .env file
COOKIE_DOMAIN=localhost
COOKIE_SECURE=false
COOKIE_HTTP_ONLY=true
```

**Test Steps**:

1. **Start application** (use IntelliJ IDEA)
2. **Test Kakao Login**:
```bash
curl -i http://localhost:8080/login/kakao \
  -H "Origin: http://localhost:3000" \
  -v 2>&1 | grep -E "(Set-Cookie|Location|HTTP)"
```

Expected:
```
HTTP/1.1 302
Location: /oauth2/authorization/kakao
Set-Cookie: REQUEST_DOMAIN=localhost; Path=/; HttpOnly; SameSite=None
```

3. **Test Apple Login**:
```bash
curl -i http://localhost:8080/login/apple \
  -H "Origin: http://localhost:3000" \
  -v 2>&1 | grep -E "(Set-Cookie|Location|HTTP)"
```

Expected:
```
HTTP/1.1 302
Location: /oauth2/authorization/apple
Set-Cookie: REQUEST_DOMAIN=localhost; Path=/; HttpOnly; SameSite=None
```

4. **Test Apple Login V1 (POST)**:
```bash
curl -i http://localhost:8080/v1/auth/login/apple \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:3000" \
  -d '{"authorizationCode": "test_code"}' \
  -v 2>&1 | grep -E "(Set-Cookie|HTTP)"
```

Expected:
```
HTTP/1.1 200
Set-Cookie: accessToken=...; Path=/; HttpOnly; SameSite=None
Set-Cookie: refreshToken=...; Path=/; HttpOnly; SameSite=None
```

5. **Verify Cookies in Browser**:
   - Open Chrome DevTools → Application → Cookies
   - Verify `REQUEST_DOMAIN`, `accessToken`, `refreshToken` are set

#### 5.2 Develop Environment Testing

**Prerequisites**:
```bash
# CI/CD Environment variables
COOKIE_DOMAIN=depromeet.shop  # No leading dot
COOKIE_SECURE=true
COOKIE_HTTP_ONLY=false
```

**Test Steps**:

1. **Deploy to develop**
2. **Test REQUEST_DOMAIN cookie**:
```bash
curl -i https://api.depromeet.shop/login/apple \
  -H "Origin: https://core.depromeet.shop" \
  -v 2>&1 | grep -E "(Set-Cookie|Location|HTTP)"
```

Expected:
```
HTTP/1.1 302
Location: /oauth2/authorization/apple
Set-Cookie: REQUEST_DOMAIN=core.depromeet.shop; Domain=depromeet.shop; Path=/; Secure; HttpOnly; SameSite=None
```

3. **Verify Cookie Domain**:
   - Cookie `Domain` attribute should be `depromeet.shop` (no leading dot)
   - Cookie should be accessible on `core.depromeet.shop`, `admin.depromeet.shop`, `api.depromeet.shop`

4. **Test Full OAuth2 Flow**:
   - Complete Apple OAuth2 login in browser
   - Verify `refreshToken` cookie is set
   - Verify `accessToken` cookie is set
   - Verify cookies are accessible on frontend (`core.depromeet.shop`)

5. **Check Logs**:
   - No more `IllegalArgumentException: An invalid domain` errors
   - No more `Failed to set REQUEST_DOMAIN cookie` warnings

#### 5.3 Production Environment Testing

**Prerequisites**:
```bash
# CI/CD Environment variables
COOKIE_DOMAIN=depromeet.com  # No leading dot
COOKIE_SECURE=true
COOKIE_HTTP_ONLY=false
```

**Test Steps**:
1. **Deploy to production** (after develop verification)
2. **Same tests as develop environment**
3. **Monitor logs for errors**

---

## Implementation Checklist

### Environment Configuration

- [ ] Update `COOKIE_DOMAIN` in develop CI/CD pipeline (`.depromeet.shop` → `depromeet.shop`)
- [ ] Update `COOKIE_DOMAIN` in production CI/CD pipeline (`.depromeet.com` → `depromeet.com`)
- [ ] Update `.env.example` with correct format (no leading dot)
- [ ] Verify `COOKIE_SECURE=true` for develop and production
- [ ] Verify `COOKIE_HTTP_ONLY=false` for develop and production

### Code Updates

- [ ] Update `createCookie()` method in `MemberLoginController.kt`:
  - [ ] Set `domain` from configuration (except localhost)
  - [ ] Set `secure` from configuration
  - [ ] Set `SameSite=None` for all cookies

### CORS Verification

- [ ] Find CORS configuration file
- [ ] Verify CORS allows credentials (`allowCredentials(true)`)
- [ ] Verify CORS allows all frontend origins
- [ ] Update CORS if needed

### Documentation

- [ ] Update `AUTOMATION_GUIDE.md` with cookie domain rules
- [ ] Add cookie configuration section to `.env.example`
- [ ] Document environment variables in README

### Testing

- [ ] Local testing (all three endpoints)
- [ ] Develop environment testing (full OAuth2 flow)
- [ ] Verify refreshToken is saved correctly
- [ ] Verify cookies work across subdomains
- [ ] Production environment testing (after develop verification)

---

## Rollback Plan

If the fix causes issues:

1. **Revert environment variables**:
   ```bash
   # Rollback
   export COOKIE_DOMAIN=.depromeet.shop  # Revert to old value
   ```

2. **Use code-level fix** instead:
   - Detect and strip leading dot in code
   - See alternative implementation below

### Alternative Implementation (Code-Level Fix)

If environment variable changes are not possible, fix in code:

```kotlin
private fun setCookie(request: HttpServletRequest, response: HttpServletResponse) {
    try {
        val requestDomain =
            URI(request.getHeader(ORIGIN) ?: request.getHeader(REFERER) ?: request.requestURL.toString()).host

        Cookie(REQUEST_DOMAIN, requestDomain).apply {
            path = "/"

            // Strip leading dot from domain if present
            val cookieDomain = securityProperties.cookie.domain.let {
                if (it.startsWith(".")) it.substring(1) else it
            }
            domain = cookieDomain

            maxAge = 60
            isHttpOnly = true
            secure = true
            setAttribute("SameSite", "None")
            response.addCookie(this)
        }
    } catch (e: Exception) {
        logger.warn(e) { "Failed to set REQUEST_DOMAIN cookie : ${e.message}" }
    }
}

private fun createCookie(name: String, value: String, maxAgeSeconds: Int): Cookie {
    return Cookie(name, value).apply {
        path = "/"

        // Strip leading dot from domain if present
        val cookieDomain = securityProperties.cookie.domain.let {
            if (it.startsWith(".")) it.substring(1) else it
        }

        domain = if (cookieDomain != "localhost") cookieDomain else null
        maxAge = maxAgeSeconds
        isHttpOnly = true
        secure = securityProperties.cookie.secure
        setAttribute("SameSite", "None")
    }
}
```

**Pros of Code-Level Fix**:
- No environment variable changes needed
- Works even if someone adds leading dot by mistake
- Self-healing code

**Cons**:
- Not the "proper" fix (environment should be correct)
- Adds complexity to code

---

## Success Criteria

### Cookie Domain Validation

- [ ] No more `IllegalArgumentException: An invalid domain` errors
- [ ] REQUEST_DOMAIN cookie set successfully
- [ ] RefreshToken cookie set successfully
- [ ] AccessToken cookie set successfully

### Cross-Subdomain Cookie Sharing

- [ ] Cookies set on `api.depromeet.shop` accessible on `core.depromeet.shop`
- [ ] Cookies set on `api.depromeet.shop` accessible on `admin.depromeet.shop`
- [ ] Cookies work across all subdomains

### OAuth2 Login Flow

- [ ] Apple OAuth2 login completes successfully
- [ ] Kakao OAuth2 login completes successfully
- [ ] RefreshToken is saved in cookie after login
- [ ] Frontend can access refreshToken cookie
- [ ] User stays logged in across page refreshes

### CORS Configuration

- [ ] CORS preflight requests succeed
- [ ] Credentials (cookies) included in CORS responses
- [ ] No CORS errors in browser console

---

## Verification Commands

### Check Cookie Domain in Response

```bash
# Develop
curl -i https://api.depromeet.shop/login/apple \
  -H "Origin: https://core.depromeet.shop" \
  2>&1 | grep -i "set-cookie"

# Expected output:
# Set-Cookie: REQUEST_DOMAIN=core.depromeet.shop; Domain=depromeet.shop; ...
```

### Verify Cookie Attributes in Browser

1. Open Chrome DevTools
2. Navigate to Application → Cookies
3. Click on `api.depromeet.shop`
4. Verify cookies:
   - **Name**: REQUEST_DOMAIN, accessToken, refreshToken
   - **Domain**: `depromeet.shop` (no leading dot)
   - **Path**: `/`
   - **Secure**: ✅ (for dev/prod)
   - **HttpOnly**: ✅
   - **SameSite**: None

### Test Cross-Subdomain Access

```javascript
// Run in browser console on https://core.depromeet.shop
console.log(document.cookie);
// Should include: REQUEST_DOMAIN=..., accessToken=..., refreshToken=...
```

---

## Deployment Plan

### Step 1: Deploy to Develop

1. Update CI/CD environment variables
2. Deploy to develop environment
3. Test OAuth2 login flow
4. Verify cookies are set correctly
5. Test cross-subdomain cookie sharing
6. Monitor logs for errors

### Step 2: Verify and Monitor

1. Monitor error logs for 24 hours
2. Check for cookie-related errors
3. Verify login success rate
4. Gather user feedback

### Step 3: Deploy to Production

1. Update CI/CD environment variables for production
2. Deploy during low-traffic period
3. Monitor logs closely
4. Have rollback plan ready
5. Verify production deployment

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **Cookies not shared across subdomains** | Low | High | Test thoroughly in develop; modern browsers support domain without leading dot |
| **CORS errors** | Low | Medium | Verify CORS allows credentials and specific origins |
| **Local development issues** | Medium | Low | Keep `COOKIE_DOMAIN=localhost` for local |
| **Rollback needed** | Low | Medium | Have rollback plan ready; can revert environment variables |
| **Browser compatibility** | Very Low | High | All modern browsers (Chrome, Firefox, Safari) support domain without leading dot |

**Overall Risk**: **LOW**

**Reasoning**:
- Configuration change only (no complex code changes)
- Well-documented RFC6265 standard
- Modern browsers fully support this approach
- Easy to rollback if needed

---

## References

### Research Document
- `thoughts/shared/research/2025-01-18-cookie-domain-validation-error-research.md`

### Related Plans
- `thoughts/shared/plans/2025-01-18-swagger-controller-visibility-fix-plan.md`
- `thoughts/shared/plans/2026-01-18-apple-oauth2-fix-implementation-plan.md`

### Standards and Specifications
- [RFC 6265 - HTTP State Management Mechanism](https://datatracker.ietf.org/doc/html/rfc6265)
- [RFC 6265 - Section 4.1.2.3 (The Domain Attribute)](https://datatracker.ietf.org/doc/html/rfc6265#section-4.1.2.3)

### External Resources
- [The Cookie Processor Component - Apache Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/config/cookie-processor.html)
- [MDN HTTP Cookies](https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies)
- [Spring Boot CORS Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.servlet.spring-mvc.cors)

---

## Next Steps

1. **Review this plan** with team
2. **Locate CORS configuration** in codebase
3. **Update environment variables** in CI/CD pipeline
4. **Update cookie creation code** (if needed)
5. **Deploy to develop** and test
6. **Monitor and verify**
7. **Deploy to production**

---

## Summary

**Problem**: Cookie domain validation error prevents refreshToken from being saved after OAuth2 login

**Root Cause**: `COOKIE_DOMAIN` environment variable has leading dot (`.depromeet.shop`), violating RFC6265

**Solution**:
1. Remove leading dot from `COOKIE_DOMAIN` in CI/CD pipeline
2. Update cookie creation to use domain from configuration
3. Verify CORS configuration allows credentials
4. Test thoroughly in develop, then production

**Expected Outcome**:
- ✅ REQUEST_DOMAIN cookie set successfully
- ✅ RefreshToken cookie saved after OAuth2 login
- ✅ Cookies shared across subdomains
- ✅ OAuth2 login flow works correctly

**Risk**: Low - simple configuration change, easy to rollback

---

**Last Updated**: 2025-01-18 15:00:00 KST
**Status**: Ready for Implementation
**Estimated Time**: 2-3 hours (including testing)
