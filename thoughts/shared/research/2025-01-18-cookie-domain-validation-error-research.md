---
date: 2025-01-18T14:30:00+09:00
researcher: claude
issue: Cookie Domain Validation Error in Develop/Prod
ticket: "Cookie Domain Fix"
branch: refactor/cookie-domain-fix
repository: dpm-core-server
status: research_complete
---

# Research: Cookie Domain Validation Error

**Date**: 2025-01-18 14:30:00 KST
**Researcher**: claude
**Issue**: REQUEST_DOMAIN cookie fails after PR #232 merge to develop
**Error**: `An invalid domain [.depromeet.shop] was specified for this cookie`
**Goal**: Fix cookie domain validation for develop and prod environments

---

## Problem Statement

### Current Error

```java
java.lang.IllegalArgumentException: An invalid domain [.depromeet.shop] was specified for this cookie
    at org.apache.tomcat.util.http.Rfc6265CookieProcessor.validateDomain(Rfc6265CookieProcessor.java:253)
    at org.apache.tomcat.util.http.Rfc6265CookieProcessor.parseCookieHeader(Rfc6265CookieProcessor.java:175)
    at org.apache.catalina.connector.Request.parseCookies(Request.java:3032)
    at org.apache.catalina.connector.Request.getCookies(Request.java:2050)
```

**Impact**: RefreshToken not being saved after Apple OAuth2 login in develop/prod environments.

### Root Cause

In `MemberLoginController.setCookie()` (lines 96-113):

```kotlin
private fun setCookie(request: HttpServletRequest, response: HttpServletResponse) {
    try {
        val requestDomain =
            URI(request.getHeader(ORIGIN) ?: request.getHeader(REFERER) ?: request.requestURL.toString()).host
        // Extracted host: ".depromeet.shop" (with LEADING DOT)

        Cookie(REQUEST_DOMAIN, requestDomain).apply {
            path = "/"
            domain = securityProperties.cookie.domain  // COOKIE_DOMAIN env var
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

**The Issue**:
1. `COOKIE_DOMAIN` environment variable is likely set to `.depromeet.shop` (with leading dot)
2. Tomcat's `Rfc6265CookieProcessor` validates cookie domains per RFC6265 standard
3. **RFC6265 prohibits domain attributes starting with a dot**
4. Cookie setting fails → REQUEST_DOMAIN cookie not set → refreshToken cannot be saved

---

## Technical Background

### RFC6265 Cookie Domain Rules

**RFC6265 Section 4.1.2.3** (The Domain Attribute):

> If the attribute-name case-insensitively matches the string "Domain", the
> user agent MUST behave as follows:
> 1. Let the cookie-domain be the attribute-value **without the leading %x2E (".") character**.
> 2. If the cookie-domain does not start with a letter or digit, **fail the cookie**.

**Key Points**:
- Domain names **MUST NOT** start with a dot (.) character
- Leading dots should be **stripped** before validation
- If domain starts with a dot, cookie **fails validation**

### Tomcat's Rfc6265CookieProcessor

**Tomcat 8+** uses `Rfc6265CookieProcessor` by default (instead of `LegacyCookieProcessor`).

**Validation Logic** (simplified):
```java
// From Rfc6265CookieProcessor.validateDomain()
if (domain.startsWith(".")) {
    throw new IllegalArgumentException("An invalid domain [" + domain + "] was specified for this cookie");
}
```

**Migration from LegacyCookieProcessor**:
- **LegacyCookieProcessor**: Allowed leading dots (RFC2965 behavior)
- **Rfc6265CookieProcessor**: Strict validation, rejects leading dots (RFC6265)

### Browser Behavior (The Paradox)

**Modern browsers** (Chrome, Firefox, Safari) actually **require** the leading dot:
- Without leading dot: Cookie only sent to **exact domain**
- With leading dot: Cookie sent to **domain + all subdomains**

**Example**:
- Domain: `.depromeet.shop` → Cookie sent to `depromeet.shop`, `core.depromeet.shop`, `api.depromeet.shop`
- Domain: `depromeet.shop` → Cookie **only** sent to `depromeet.shop` (exact match)

---

## Environment Configuration Analysis

### Current Configuration

**File**: `application.yml` (lines 32-35)

```yaml
spring:
  security:
    cookie:
      domain: ${COOKIE_DOMAIN:localhost}
      secure: ${COOKIE_SECURE:true}
      http-only: ${COOKIE_HTTP_ONLY:false}
```

**CORS Allowed Origins** (lines 85-96):

```yaml
cors:
  allowed-origins:
    - https://core.depromeet.shop    # Develop
    - https://admin.depromeet.shop   # Develop
    - https://api.depromeet.shop     # Develop
    - https://core.depromeet.com     # Prod
    - https://admin.depromeet.com    # Prod
    - https://api.depromeet.com      # Prod
```

### Environment Variables (Likely Configuration)

| Environment | COOKIE_DOMAIN | Expected Behavior |
|-------------|---------------|-------------------|
| **LOCAL** | `localhost` | ✅ Works (no leading dot) |
| **DEV** | `.depromeet.shop` ❌ | ❌ Fails validation |
| **PROD** | `.depromeet.com` ❌ | ❌ Fails validation |

---

## Solution Options

### Option 1: Remove Leading Dot from Environment Variables ⭐ RECOMMENDED

**Change**: Update `COOKIE_DOMAIN` environment variable to **remove leading dot**

**Configuration Changes**:

| Environment | Current | New | Expected Result |
|-------------|---------|-----|-----------------|
| **DEV** | `.depromeet.shop` | `depromeet.shop` | ✅ Cookie works for subdomains |
| **PROD** | `.depromeet.com` | `depromeet.com` | ✅ Cookie works for subdomains |

**Why This Works**:
- Tomcat's `Rfc6265CookieProcessor` strips the leading dot **automatically**
- Browsers internally prepend the dot when setting cookies
- Modern browsers accept domain without leading dot and still share with subdomains

**Deployment Changes**:
```bash
# Develop environment
COOKIE_DOMAIN=depromeet.shop  # No leading dot

# Prod environment
COOKIE_DOMAIN=depromeet.com   # No leading dot
```

**Pros**:
- Simple configuration change
- No code changes needed
- RFC6265 compliant
- Works with all modern browsers

**Cons**:
- Requires environment variable update in CI/CD pipeline
- Need to verify browser behavior (see verification section below)

---

### Option 2: Use ResponseCookie Instead of Cookie 🔄 ALTERNATIVE

**Change**: Replace `jakarta.servlet.http.Cookie` with Spring's `ResponseCookie`

**Code Changes** in `MemberLoginController.kt`:

```kotlin
import org.springframework.http.ResponseCookie

private fun setCookie(request: HttpServletRequest, response: HttpServletResponse) {
    try {
        val requestDomain =
            URI(request.getHeader(ORIGIN) ?: request.getHeader(REFERER) ?: request.requestURL.toString()).host

        // Use ResponseCookie (Spring 6+ handles leading dots correctly)
        val cookie = ResponseCookie.from(REQUEST_DOMAIN, requestDomain)
            .domain(securityProperties.cookie.domain)  // Can handle leading dot
            .path("/")
            .maxAge(Duration.ofMinutes(1))
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
    } catch (e: Exception) {
        logger.warn(e) { "Failed to set REQUEST_DOMAIN cookie : ${e.message}" }
    }
}

private fun addTokenCookies(response: HttpServletResponse, tokens: AuthTokenResponse) {
    val accessTokenCookie = createResponseCookie("accessToken", tokens.accessToken, 60 * 60 * 24)
    val refreshTokenCookie = createResponseCookie("refreshToken", tokens.refreshToken, 60 * 60 * 24 * 30)

    response.addHeader("Set-Cookie", accessTokenCookie.toString())
    response.addHeader("Set-Cookie", refreshTokenCookie.toString())
}

private fun createResponseCookie(name: String, value: String, maxAgeSeconds: Int): ResponseCookie {
    return ResponseCookie.from(name, value)
        .path("/")
        .maxAge(Duration.ofSeconds(maxAgeSeconds.toLong()))
        .httpOnly(true)
        .secure(true)
        .sameSite("None")
        .build()
}
```

**Pros**:
- Modern Spring 6+ approach
- Better API for SameSite, Secure, HttpOnly attributes
- More flexible with domain handling

**Cons**:
- More code changes
- Need to update all cookie creation methods
- Still requires domain without leading dot

---

### Option 3: Configure LegacyCookieProcessor ⚠️ NOT RECOMMENDED

**Change**: Configure Tomcat to use legacy cookie processor

**Configuration** in `TomcatConfig.kt`:

```kotlin
import org.apache.catalina.Context
import org.apache.tomcat.util.http.LegacyCookieProcessor
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TomcatConfig {

    @Bean
    fun cookieProcessorCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            factory.addContextCustomizers { context: Context ->
                context.cookieProcessor = LegacyCookieProcessor()
            }
        }
    }
}
```

**Pros**:
- Allows leading dots in domain
- Backward compatible with old code

**Cons**:
- ⚠️ **Not RFC6265 compliant**
- Deprecated approach
- May cause issues with modern browsers
- Not recommended for new projects

---

## Recommended Solution: Option 1

### Implementation Plan

#### Step 1: Update Environment Variables

**For Develop Environment**:
```bash
# CI/CD Pipeline (GitHub Actions, AWS CodeDeploy, etc.)
export COOKIE_DOMAIN=depromeet.shop  # Remove leading dot
```

**For Prod Environment**:
```bash
# CI/CD Pipeline
export COOKIE_DOMAIN=depromeet.com   # Remove leading dot
```

#### Step 2: Update Documentation

Update `.env` example file to show correct format:

```bash
# .env.example

# Cookie Domain Configuration
# IMPORTANT: Do NOT use leading dot (e.g., .depromeet.shop)
# RFC6265 prohibits domains starting with dots
COOKIE_DOMAIN=depromeet.shop  # Correct
# COOKIE_DOMAIN=.depromeet.shop  # WRONG - will fail validation
```

#### Step 3: Verify Code

The current code in `MemberLoginController.kt` should work correctly after environment variable update:

```kotlin
Cookie(REQUEST_DOMAIN, requestDomain).apply {
    path = "/"
    domain = securityProperties.cookie.domain  // Now "depromeet.shop" (no leading dot)
    maxAge = 60
    isHttpOnly = true
    secure = true
    setAttribute("SameSite", "None")
    response.addCookie(this)
}
```

#### Step 4: Update AUTOMATION_GUIDE.md

Add cookie domain configuration section:

```markdown
### Cookie Domain Configuration

**RFC6265 Compliance**: Cookie domains MUST NOT start with a dot

**Correct Format**:
- Local: `COOKIE_DOMAIN=localhost`
- Dev: `COOKIE_DOMAIN=depromeet.shop`
- Prod: `COOKIE_DOMAIN=depromeet.com`

**Incorrect Format** (will cause validation error):
- Dev: `COOKIE_DOMAIN=.depromeet.shop` ❌
- Prod: `COOKIE_DOMAIN=.depromeet.com` ❌
```

---

## Verification Plan

### Test 1: Cookie Domain Validation

**Test in Local**:
```kotlin
@Test
fun `setCookie should succeed with domain without leading dot`() {
    // Arrange
    val request = mockHttpServletRequest()
    val response = mockHttpServletResponse()

    // Act
    controller.setCookie(request, response)

    // Assert
    val cookies = response.cookies
    assertThat(cookies).isNotNull()
    assertThat(cookies).anyMatch { it.name == "REQUEST_DOMAIN" }
}
```

### Test 2: Browser Subdomain Sharing

**Test in Browser**:
1. Set cookie on `https://api.depromeet.shop` with domain `depromeet.shop`
2. Navigate to `https://core.depromeet.shop`
3. Verify cookie is accessible (using dev tools)
4. Repeat for prod environment

**Expected Result**: Cookie should be shared across subdomains without leading dot.

### Test 3: Full OAuth2 Flow

**Test in Develop Environment**:
```bash
# 1. Start OAuth2 login flow
curl -i https://api.depromeet.shop/login/apple \
  -H "Origin: https://core.depromeet.shop"

# 2. Check response for Set-Cookie header
# Expected: Set-Cookie: REQUEST_DOMAIN=core.depromeet.shop; Domain=depromeet.shop; ...

# 3. Complete OAuth2 flow
# 4. Verify refreshToken is saved in cookie
```

---

## Browser Compatibility

### RFC6265 Browser Behavior

According to [MDN documentation issue](https://github.com/mdn/content/issues/32050) (February 2024):

| Browser | Domain Without Dot | Domain With Dot |
|---------|-------------------|-----------------|
| **Chrome** | ✅ Shares with subdomains | ✅ Shares with subdomains |
| **Firefox** | ✅ Shares with subdomains | ✅ Shares with subdomains |
| **Safari** | ✅ Shares with subdomains | ✅ Shares with subdomains |

**Key Finding**: Modern browsers **automatically treat domains as domain cookies**, so the leading dot is not necessary.

**Conclusion**: Using domain without leading dot (`depromeet.shop`) is:
- ✅ RFC6265 compliant
- ✅ Accepted by Tomcat's Rfc6265CookieProcessor
- ✅ Works with all modern browsers
- ✅ Shares cookies across subdomains

---

## Additional Context: Why It Worked Before

### Before PR #232

The `setCookie` method likely didn't exist or was different. The refresh tokens may have been handled differently.

### After PR #232

The `setCookie` method was introduced (or modified) to:
1. Extract domain from Origin/Referer headers
2. Set REQUEST_DOMAIN cookie for OAuth2 flow
3. Use `securityProperties.cookie.domain` for cookie domain attribute

**Trigger**: The `COOKIE_DOMAIN` environment variable in develop/prod has a leading dot (`.depromeet.shop`), causing validation to fail.

---

## Migration Checklist

- [ ] Update `COOKIE_DOMAIN` in develop environment (remove leading dot)
- [ ] Update `COOKIE_DOMAIN` in prod environment (remove leading dot)
- [ ] Update CI/CD pipeline configuration
- [ ] Update `.env.example` documentation
- [ ] Update `AUTOMATION_GUIDE.md` with cookie domain rules
- [ ] Test in local environment
- [ ] Test in develop environment after deployment
- [ ] Verify refreshToken is saved correctly
- [ ] Verify cookies are shared across subdomains
- [ ] Test full OAuth2 flow (Kakao and Apple)

---

## References

### RFC6265 Documentation
- [RFC 6265 - Section 4.1.2.3 (The Domain Attribute)](https://datatracker.ietf.org/doc/html/rfc6265#section-4.1.2.3)

### Tomcat Documentation
- [The Cookie Processor Component - Apache Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/config/cookie-processor.html)

### Spring Framework Issues
- [ResponseCookie should ignore leading dot in domain (Issue #23776)](https://github.com/spring-projects/spring-framework/issues/23776)
- [ResponseCookie to allow leading dot in domain name again (Issue #23924)](https://github.com/spring-projects/spring-framework/issues/23924)

### Community Discussions
- [Tomcat cookie domain validation - Stack Overflow](https://stackoverflow.com/questions/29608550/tomcat-cookie-domain-validation)
- [Set-Cookie domain attribute actually needs leading dot - MDN (February 2024)](https://github.com/mdn/content/issues/32050)
- [Leading Dots On HTTP Cookie Domains Ignored - Ben Nadel](https://www.bennadel.com/blog/4345-leading-dots-on-http-cookie-domains-ignored.htm)

### Chinese Resources
- [Tomcat cookie domain validation 问题修复方案](https://blog.csdn.net/fy_sun123/article/details/73115381)
- [Tomcat中LegacyCookieProcessor与 Rfc6265CookieProcessor](https://blog.csdn.net/zzhongcy/article/details/122981590)

---

## Next Steps

1. **Review this research document** with team
2. **Decide on implementation approach** (Option 1 recommended)
3. **Update environment variables** in CI/CD pipeline
4. **Test thoroughly** in develop environment
5. **Create PR** with documentation updates
6. **Deploy to develop** and verify fix
7. **Deploy to prod** after develop verification

---

## Summary

**Root Cause**: `COOKIE_DOMAIN` environment variable has leading dot (`.depromeet.shop`), violating RFC6265.

**Solution**: Remove leading dot from `COOKIE_DOMAIN` environment variable.

**Impact**: REQUEST_DOMAIN cookie will be set successfully, allowing refreshToken to be saved after OAuth2 login.

**Risk**: Low - Modern browsers accept domains without leading dots and still share cookies across subdomains.

---

**Last Updated**: 2025-01-18 14:30:00 KST
**Status**: Research Complete - Ready for Implementation
