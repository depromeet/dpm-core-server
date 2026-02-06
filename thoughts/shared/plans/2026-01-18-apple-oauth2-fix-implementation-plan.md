---
date: 2026-01-18T15:00:00+09:00
planner: claude
based_on_research: 2026-01-18-apple-oauth2-500-error-investigation.md
ticket: "#232 - Apple OAuth 2.0 500 Error Fix"
branch: refactor/#232
repository: dpm-core-server
status: ready_for_implementation
---

# 구현 계획: Apple OAuth 2.0 500 에러 해결

**날짜**: 2026-01-18 15:00:00 KST
**작성자**: claude
**기반 연구**: `2026-01-18-apple-oauth2-500-error-investigation.md`
**티켓**: #232 - Apple OAuth 2.0 500 Error Fix

## 계획 개요

이 계획은 Apple Sign In 웹 표준을 우선적으로 고려하여, 현재 prod 환경에서 발생하는 Apple OAuth 2.0 500 에러를 해결합니다.

**중요사항**:
- **Dev/Prod 배포는 PR merge to develop/main 브랜치 조건에서만 수행**
- **현재 상황에서는 LOCAL 환경에서만 테스트 가능**
- **검증은 curl 명령어를 통한 실제 요청으로 수행**
- **@Controller를 유지하면서 Swagger API 문서 표시 문제 해결**

### 핵심 발견

1. **현재 구현은 표준 준수**: Apple의 공식 OAuth2 Authorization Code Flow를 정확히 구현
2. **문제는 설정 병합**: Spring Boot YAML 병합 메커니즘이 `oauth2.client` 레벨에서 전체를 덮어씌움
3. **해결책 적용 완료**: 이전 작업에서 이미 설정 파일 분리 완료

### 배포 전략

| 환경 | 배포 조건 | 테스트 가능 여운 |
|------|-----------|-----------------|
| **LOCAL** | 언제나 가능 | ✅ 가능 |
| **DEV** | PR merge to `develop` 브랜치 시 | ⚠️ 배포 후만 가능 |
| **PROD** | PR merge to `main` 브랜치 시 | ⚠️ 배포 후만 가능 |

### Apple Sign In 웹 표준 검증

**표준 OAuth2 Authorization Code Flow** (Apple 공식 문서 기반):
1. 사용자가 "Sign in with Apple" 버튼 클릭
2. Apple 인증 페이지로 리다이렉션 (`authorization-uri`)
3. 사용자 인증 후 authorization code 수신
4. Authorization code로 access token 교환 (`token-uri`)
5. JWT 기반 client secret 사용 (ES256 알고리즘)
6. ID 토큰 검증 및 사용자 정보 추출

**현재 구현과의 비교**:

| 단계 | Apple 표준 | 현재 구현 | 상태 |
|------|------------|-----------|------|
| 인증 요청 | `appleid.apple.com/auth/authorize` | `application-prod.yml:276` | ✅ 일치 |
| 토큰 교환 | `appleid.apple.com/auth/token` | `application-prod.yml:277` | ✅ 일치 |
| 클라이언트 시크릿 | JWT (ES256) | `AppleClientSecretGenerator.kt` | ✅ 표준 준수 |
| ID 토큰 검증 | 공개키로 서명 검증 | `AppleIdTokenValidator.kt` | ✅ 표준 준수 |
| 사용자 정보 | `sub`, `email` | `AppleAuthAttributes.kt` | ✅ 표준 준수 |

**결론**: 현재 코드 구현은 Apple의 웹 표준을 완벽하게 따르고 있음. 수정이 필요한 부분은 설정 구조뿐입니다.

---

## 사전 요구사항

### 필수 조건

- [x] Git 브랜치 `refactor/#232`에서 작업
- [x] 연구 문서 검토 완료
- [ ] 모든 환경 변수가 각 profile에 설정됨
  - `DEV_APPLE_CLIENT_ID`, `DEV_APPLE_KEY_ID`, `DEV_APPLE_TEAM_ID`, `DEV_APPLE_PRIVATE_KEY`, `DEV_APPLE_REDIRECT_URI`
  - `PROD_APPLE_CLIENT_ID`, `PROD_APPLE_KEY_ID`, `PROD_APPLE_TEAM_ID`, `PROD_APPLE_PRIVATE_KEY`, `PROD_APPLE_REDIRECT_URI`
  - `LOCAL_APPLE_CLIENT_ID`, `LOCAL_APPLE_KEY_ID`, `LOCAL_APPLE_TEAM_ID`, `LOCAL_APPLE_PRIVATE_KEY`, `LOCAL_APPLE_REDIRECT_URI`
  - Kakao 관련 환경 변수들 (기본 설정에 필요)

### 영향 범위

**설정 파일**:
- `application/src/main/resources/application.yml` (기본 설정 - Kakao만 유지)
- `application/src/main/resources/application-local.yml` (Apple 설정 추가 완료)
- `application/src/main/resources/application-dev.yml` (Apple 설정 추가 완료)
- `application/src/main/resources/application-prod.yml` (Apple 설정 추가 완료)

**코드 파일** (수정 불필요, 이미 표준 준수):
- `CustomOAuth2UserService.kt`
- `CustomOAuth2AccessTokenResponseClient.kt`
- `AppleClientSecretGenerator.kt`
- `AppleIdTokenValidator.kt`
- `SecurityConfig.kt`
- `MemberLoginController.kt` (@Controller 유지하며 Swagger 문서화 추가)

---

## 구현 단계

### Phase 0: @Controller Swagger 표시 문제 해결

**목표**: `@Controller`를 사용하는 `MemberLoginController`가 Swagger UI에 표시되도록 수정

**문제 설명**:
- 현재 `MemberLoginController`는 `@Controller`를 사용하여 OAuth2 리다이렉션을 처리
- `@Controller`는 리다이렉션 문자열을 뷰 이름으로 반환하므로 브라우저에서 정상 리다이렉션됨
- 하지만 Swagger(Springdoc OpenAPI)는 `@Controller`를 API 엔드포인트로 인식하지 않음
- `@RestController`로 변경하면 리다이렉션이 텍스트로 렌더링되어 기능이 깨짐

**해결 방법**: Springdoc OpenAPI가 `@Controller`도 스캔하도록 설정

#### 0.1 OpenAPI 설정 확인

**파일**: `application/src/main/kotlin/core/application/security/configuration/OpenApiConfig.kt` (또는 해당 설정 파일)

**확인할 내용**:
```kotlin
// Springdoc이 @Controller도 스캔하도록 설정
@Bean
fun OpenApiCustomiser(): OpenApiCustomiser {
    return OpenApiCustomiser { openApi: OpenAPI ->
        // MemberLoginController의 엔드포인트를 명시적으로 추가
        val paths = openApi.paths ?: Paths()

        // GET /login/kakao - Kakao OAuth2 시작
        val kakaoLogin = PathItem()
            .get(Operation().apply {
                summary("Kakao OAuth2 Login Redirect")
                description("Kakao OAuth2 인증을 시작합니다. 리다이렉션을 반환합니다.")
                operationId("kakaoLogin")
                tags(setOf("Member-Login"))
                responses(
                    mapOf(
                        "302" to Response().description("Found - Redirect to Kakao OAuth2 authorization page"),
                        "400" to Response().description("Bad Request")
                    )
                )
            })

        // GET /login/apple - Apple OAuth2 시작
        val appleLogin = PathItem()
            .get(Operation().apply {
                summary("Apple OAuth2 Login Redirect")
                description("Apple OAuth2 인증을 시작합니다. 리다이렉션을 반환합니다.")
                operationId("appleLogin")
                tags(setOf("Member-Login"))
                responses(
                    mapOf(
                        "302" to Response().description("Found - Redirect to Apple OAuth2 authorization page"),
                        "400" to Response().description("Bad Request")
                    )
                )
            })

        // POST /v1/auth/login/apple - Apple 로그인 v1
        val appleLoginV1 = PathItem()
            .post(Operation().apply {
                summary("Apple OAuth2 Login V1")
                description("Apple authorization code로 로그인합니다.")
                operationId("appleLoginV1")
                tags(setOf("Member-Login"))
                requestBody(
                    RequestBody().apply {
                        content(
                            mapOf(
                                "application/json" to Content().apply {
                                    schema(Schema<AppleLoginRequest>().apply {
                                        type("object")
                                        addPropertyItem("authorizationCode", StringSchema()._required(listOf("authorizationCode")))
                                    })
                                }
                            )
                        )
                    }
                )
                responses(
                    mapOf(
                        "200" to Response().description("OK - Login successful"),
                        "401" to Response().description("Unauthorized - Invalid authorization code"),
                        "500" to Response().description("Internal Server Error")
                    )
                )
            })

        paths.addPathItem("/login/kakao", kakaoLogin)
        paths.addPathItem("/login/apple", appleLogin)
        paths.addPathItem("/v1/auth/login/apple", appleLoginV1)

        openApi.paths(paths)
    }
}
```

**검증 방법**:
```bash
# 애플리케이션 시작
./gradlew :application:bootRun --args='--spring.profiles.active=local'

# Swagger UI 접속
open http://localhost:8080/swagger-ui/index.html

# 또는 OpenAPI JSON 확인
curl http://localhost:8080/v3/api-docs | jq '.paths | keys | map(select(contains("login")))'
```

**성공 기준**:
- [ ] Swagger UI에 "Member-Login" 태그가 표시됨
- [ ] `/login/kakao`, `/login/apple`, `/v1/auth/login/apple` 엔드포인트가 표시됨
- [ ] 각 엔드포인트의 설명과 파라미터가 정확히 표시됨

---

### Phase 1: 설정 수정 검증

**목표**: 이전 작업에서 적용한 설정 파일 분리가 올바르게 적용되었는지 확인

#### 1.1 기본 설정 파일 확인

**파일**: `application/src/main/resources/application.yml`

**확인할 내용**:
- [ ] `spring.security.oauth2.client`에 Kakao 설정만 존재
- [ ] Apple 관련 설정이 완전히 제거됨
- [ ] Kakao 설정이 올바른 구조로 유지됨

**검증 방법**:
```bash
cat application/src/main/resources/application.yml | grep -A 20 "spring:"
cat application/src/main/resources/application.yml | grep -i "apple"
# 두 번째 명령은 결과가 없어야 함
```

**예상 결과**:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            redirect-uri: ${KAKAO_REDIRECT_URI}
            authorization-grant-type: authorization_code
            client-authentication-method: client_secret_post
            scope:
              - account_email
              - profile_nickname
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
```

#### 1.2 Profile별 설정 파일 확인

**파일들**:
- `application/src/main/resources/application-local.yml`
- `application/src/main/resources/application-dev.yml`
- `application/src/main/resources/application-prod.yml`

**확인할 내용** (각 파일별):
- [ ] `spring.security.oauth2.client`에 Apple registration 설정 존재
- [ ] `spring.security.oauth2.client`에 Apple provider 설정 존재
- [ ] `apple.*` 커스텀 설정 존재
- [ ] 모든 환경 변수가 profile 접두사를 가짐 (예: `DEV_APPLE_CLIENT_ID`)

**검증 방법**:
```bash
# 각 profile 파일 확인
for profile in local dev prod; do
  echo "=== Checking application-${profile}.yml ==="
  cat application/src/main/resources/application-${profile}.yml | grep -A 30 "spring:"
  cat application/src/main/resources/application-${profile}.yml | grep -A 10 "^apple:"
done
```

**예상 결과** (`application-prod.yml` 예시):
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          apple:
            client-id: ${PROD_APPLE_CLIENT_ID}
            client-secret: ${PROD_APPLE_CLIENT_SECRET}
            redirect-uri: ${PROD_APPLE_REDIRECT_URI}
            authorization-grant-type: authorization_code
            client-authentication-method: client_secret_post
            scope:
              - name
              - email
        provider:
          apple:
            authorization-uri: https://appleid.apple.com/auth/authorize?response_mode=form_post
            token-uri: https://appleid.apple.com/auth/token
            jwk-set-uri: https://appleid.apple.com/auth/keys
            user-name-attribute: sub

apple:
  client-id: ${PROD_APPLE_CLIENT_ID}
  key-id: ${PROD_APPLE_KEY_ID}
  team-id: ${PROD_APPLE_TEAM_ID}
  private-key: ${PROD_APPLE_PRIVATE_KEY}
  redirect-uri: ${PROD_APPLE_REDIRECT_URI}
```

#### 1.3 빌드 및 설정 로드 테스트

**목표**: Spring Boot가 설정을 올바르게 로드하는지 확인

**검증 단계**:
```bash
# 1. Gradle 빌드로 리소스 처리 확인
./gradlew :application:processResources

# 2. Kotlin 컴파일 확인
./gradlew :application:compileKotlin

# 3. Local profile로 애플리케이션 시작 테스트 (실제 시작은 하지 않음)
./gradlew :application:bootRun --args='--spring.profiles.active=local --logging.level.org.springframework.boot.autoconfigure=DEBUG' &
PID=$!
sleep 10
kill $PID 2>/dev/null

# 4. 로그에서 OAuth2 설정 로드 확인
# 예상 로그: "Mapped source '{spring.security.oauth2.client}' to property"
```

**성공 기준**:
- [ ] 빌드 에러 없음
- [ ] Spring Boot 시작 로그에서 OAuth2 client 설정이 두 제공자(Kakao, Apple) 모두 로드됨
- [ ] 설정 병합 관련 에러 로그 없음

---

### Phase 2: 환경 변수 설정 확인

**목표**: LOCAL 환경의 환경 변수가 올바르게 설정되었는지 확인

#### 2.1 Local 환경 환경 변수 확인

**파일**: `application/src/main/resources/application-local.yml`에서 참조하는 환경 변수

**필수 환경 변수**:
```bash
# Apple
LOCAL_APPLE_CLIENT_ID
LOCAL_APPLE_KEY_ID
LOCAL_APPLE_TEAM_ID
LOCAL_APPLE_PRIVATE_KEY
LOCAL_APPLE_REDIRECT_URI

# Kakao
KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET
KAKAO_REDIRECT_URI

# 기타
JWT_SECRET_KEY
ACCESS_TOKEN_EXPIRATION_TIME
REFRESH_TOKEN_EXPIRATION_TIME
```

**검증 방법**:
```bash
# .env 파일 또는 환경 변수 확인
env | grep -E "(LOCAL_APPLE|KAKAO|JWT|TOKEN)"

# 또는 IntelliJ IDEA의 Run Configuration에서 환경 변수 확인
```

**성공 기준**:
- [ ] 모든 필수 환경 변수가 설정됨
- [ ] Apple private key가 올바른 PEM 형식임 (-----BEGIN PRIVATE KEY----- 로 시작)
- [ ] Redirect URI가 실제 로컬 개발 환경과 일치함

---

### Phase 3: LOCAL 환경 CURL 테스트

**목표**: 로컬 환경에서 Apple 및 Kakao OAuth2 로그인 엔드포인트가 정상 응답하는지 curl로 검증

**중요**: 현재 상황에서는 LOCAL 환경에서만 테스트 가능합니다.

#### 3.1 애플리케이션 시작

**단계**:
```bash
# Local profile로 애플리케이션 시작
./gradlew :application:bootRun --args='--spring.profiles.active=local'
```

**성공 기준**:
- [ ] 애플리케이션이 에러 없이 시작됨
- [ ] 로그에 "Mapped source 'spring.security.oauth2.client'" 메시지 있음
- [ ] OAuth2 client registration이 2개(Kakao, Apple) 로드됨
- [ ] 서버가 `http://localhost:8080`에서 리스닝 중

#### 3.2 Swagger UI 접근 테스트

**단계**:
```bash
# Swagger UI 접근
curl -I http://localhost:8080/swagger-ui/index.html

# 또는 브라우저에서
open http://localhost:8080/swagger-ui/index.html
```

**성공 기준**:
- [ ] HTTP 200 응답 수신
- [ ] Swagger UI가 브라우저에서 정상 렌더링됨
- [ ] "Member-Login" 태그가 표시됨 (Phase 0 구현 완료 시)
- [ ] `/login/kakao`, `/login/apple`, `/v1/auth/login/apple` 엔드포인트 표시됨

#### 3.3 Kakao 로그인 리다이렉션 테스트

**테스트 케이스 1**: Kakao 로그인 시작 (GET /login/kakao)

```bash
# 요청 - 리다이렉션 확인
curl -v http://localhost:8080/login/kakao \
  -H "Origin: http://localhost:3000" \
  -H "Referer: http://localhost:3000" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"
```

**예상 결과**:
```
< HTTP/1.1 302 Found
< Location: /oauth2/authorization/kakao
< Set-Cookie: REQUEST_DOMAIN=localhost; Path=/; Domain=localhost; HttpOnly; Secure; SameSite=None
```

**성공 기준**:
- [ ] HTTP 302 응답
- [ ] `Location` 헤더에 `/oauth2/authorization/kakao` 포함
- [ ] `REQUEST_DOMAIN` 쿠키가 설정됨
- [ ] 500 에러 없음

**테스트 케이스 2**: Kakao OAuth2 Authorization 엔드포인트

```bash
# 요청 - 실제 Kakao로 리다이렉션 확인
curl -v http://localhost:8080/oauth2/authorization/kakao \
  2>&1 | grep -E "(HTTP|Location)"
```

**예상 결과**:
```
< HTTP/1.1 302 Found
< Location: https://kauth.kakao.com/oauth/authorize?client_id=...&redirect_uri=...&response_type=code
```

**성공 기준**:
- [ ] HTTP 302 응답
- [ ] `Location` 헤더에 Kakao authorization URI 포함
- [ ] `client_id`, `redirect_uri`, `response_type=code` 파라미터 포함

#### 3.4 Apple 로그인 리다이렉션 테스트

**테스트 케이스 1**: Apple 로그인 시작 (GET /login/apple)

```bash
# 요청 - 리다이렉션 확인
curl -v http://localhost:8080/login/apple \
  -H "Origin: http://localhost:3000" \
  -H "Referer: http://localhost:3000" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"
```

**예상 결과**:
```
< HTTP/1.1 302 Found
< Location: /oauth2/authorization/apple
< Set-Cookie: REQUEST_DOMAIN=localhost; Path=/; Domain=localhost; HttpOnly; Secure; SameSite=None
```

**성공 기준**:
- [ ] HTTP 302 응답
- [ ] `Location` 헤더에 `/oauth2/authorization/apple` 포함
- [ ] `REQUEST_DOMAIN` 쿠키가 설정됨
- [ ] **500 에러가 발생하지 않음** (핵심 성공 지표)

**테스트 케이스 2**: Apple OAuth2 Authorization 엔드포인트

```bash
# 요청 - 실제 Apple로 리다이렉션 확인
curl -v http://localhost:8080/oauth2/authorization/apple \
  2>&1 | grep -E "(HTTP|Location)"
```

**예상 결과**:
```
< HTTP/1.1 302 Found
< Location: https://appleid.apple.com/auth/authorize?client_id=...&redirect_uri=...&response_mode=form_post&response_type=code
```

**성공 기준**:
- [ ] HTTP 302 응답
- [ ] `Location` 헤더에 Apple authorization URI 포함
- [ ] `client_id`, `redirect_uri`, `response_mode=form_post`, `response_type=code` 파라미터 포함
- [ ] **500 에러가 발생하지 않음** (핵심 성공 지표)

**테스트 케이스 3**: Apple 로그인 V1 API (POST /v1/auth/login/apple)

```bash
# 요청 - authorization code로 로그인
# 참고: 실제 authorization code는 Apple 로그인 플로우를 통해 얻어야 함
curl -X POST http://localhost:8080/v1/auth/login/apple \
  -H "Content-Type: application/json" \
  -d '{
    "authorizationCode": "dummy_code_for_testing"
  }' \
  -v 2>&1 | head -50
```

**예상 결과** (authorization code가 유효하지 않은 경우):
```
< HTTP/1.1 401 Unauthorized or 500 Internal Server Error
```

**성공 기준**:
- [ ] 엔드포인트가 접근 가능함 (404 아님)
- [ ] 적절한 에러 응답 (400/401/500)
- [ ] 에러 로그에 명확한 실패 원인이 표시됨

#### 3.5 기존 엔드포인트 회귀 테스트

**목표**: Apple OAuth2 추가로 기존 기능이 영향받지 않았는지 확인

**테스트 케이스 1**: Health Check

```bash
curl http://localhost:8080/actuator/health
```

**예상 결과**:
```json
{"status":"UP"}
```

**성공 기준**:
- [ ] HTTP 200 응답
- [ ] `{"status":"UP"}` 응답

**테스트 케이스 2**: Swagger API Docs

```bash
curl http://localhost:8080/v3/api-docs | jq '.paths | keys | length'
```

**성공 기준**:
- [ ] HTTP 200 응답
- [ ] API paths가 0보다 큼

#### 3.6 에러 시나리오 테스트

**테스트 케이스 1**: 잘못된 경로

```bash
curl http://localhost:8080/invalid/path
```

**예상 결과**:
```
HTTP 404 Not Found
```

**성공 기준**:
- [ ] HTTP 404 응답
- [ ] 애플리케이션이 크래시되지 않음

**테스트 케이스 2**: 인증 없는 보호 엔드포인트 접근

```bash
curl http://localhost:8080/v1/members/me
```

**예상 결과**:
```
HTTP 401 Unauthorized or 403 Forbidden
```

**성공 기준**:
- [ ] HTTP 401 또는 403 응답
- [ ] 인증이 필요함을 명확히 표시

---

### Phase 4: Dev 환경 배포 및 검증

**배포 조건**: PR이 `develop` 브랜치에 merge될 때만 자동 배포

**목표**: Dev 환경에 배포 후 OAuth2 로그인이 정상 작동하는지 확인

#### 4.1 Dev 환경 배포

**단계**:
1. PR 생성: `refactor/#232` → `develop`
2. Code review 및 승인
3. PR merge → 자동으로 Dev 배포 파이프라인 트리거
4. 배포 완료 대기

**성공 기준**:
- [ ] CI/CD 파이프라인이 성공적으로 완료됨
- [ ] Dev 서버가 정상적으로 시작됨
- [ ] Health check endpoint가 200 반환

#### 4.2 Dev 환경 CURL 테스트

**전제**: Dev 서버 URL이 `https://core.depromeet.shop`라고 가정

**테스트 케이스 1**: Apple 로그인 리다이렉션

```bash
# Dev 환경에서 Apple 로그인 테스트
curl -v https://core.depromeet.shop/login/apple \
  -H "Origin: https://core.depromeet.shop" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"
```

**성공 기준**:
- [ ] HTTP 302 응답
- [ ] Apple OAuth2 authorization으로 리다이렉션
- [ ] **500 에러 없음** (핵심 성공 지표)

**테스트 케이스 2**: Kakao 로그인 리다이렉션

```bash
curl -v https://core.depromeet.shop/login/kakao \
  -H "Origin: https://core.depromeet.shop" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"
```

**성공 기준**:
- [ ] HTTP 302 응답
- [ ] Kakao OAuth2 authorization으로 리다이렉션
- [ ] 기존 Kakao 로그인 정상 작동 (회귀 없음)

**Dev 배포 완료 조건**:
- Dev 환경에서 Apple 및 Kakao 로그인 리다이렉션 모두 성공
- 500 에러가 발생하지 않음

---

### Phase 5: Prod 환경 배포 및 최종 검증

**배포 조건**: PR이 `main` 브랜치에 merge될 때만 자동 배포

**목표**: Prod 환경에 배포 후 500 에러가 해결되었는지 최종 확인

#### 5.1 Prod 환경 배포

**단계**:
1. Dev 환경 검증 완료 후 PR 생성: `develop` → `main`
2. Code review 및 승인
3. PR merge → 자동으로 Prod 배포 파이프라인 트리거
4. 배포 완료 대기

**성공 기준**:
- [ ] CI/CD 파이프라인이 성공적으로 완료됨
- [ ] Prod 서버가 정상적으로 시작됨
- [ ] Health check endpoint가 200 반환
- [ ] 데이터베이스 마이그레이션 없음 (설정 변경만)

#### 5.2 Prod 환경 CURL 테스트

**전제**: Prod 서버 URL이 `https://core.depromeet.com`라고 가정

**테스트 케이스 1**: Apple 로그인 리다이렉션

```bash
# Prod 환경에서 Apple 로그인 테스트
curl -v https://core.depromeet.com/login/apple \
  -H "Origin: https://core.depromeet.com" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"
```

**성공 기준**:
- [ ] HTTP 302 응답
- [ ] Apple OAuth2 authorization으로 리다이렉션
- [ ] **500 에러가 발생하지 않음** ⭐ **(최종 핵심 성공 지표)**

**테스트 케이스 2**: Kakao 로그인 리다이렉션

```bash
curl -v https://core.depromeet.com/login/kakao \
  -H "Origin: https://core.depromeet.com" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"
```

**성공 기준**:
- [ ] HTTP 302 응답
- [ ] Kakao OAuth2 authorization으로 리다이렉션
- [ ] 기존 Kakao 로그인 정상 작동 (회귀 없음)

#### 5.3 모니터링 및 롤백 계획

**모니터링 항목**:
- [ ] 애플리케이션 로그에서 OAuth2 관련 에러 없음
- [ ] 에러 로그 모니터링 (Sentry, CloudWatch 등)
- [ ] 사용자 로그인 성공률 모니터링

**롤백 트리거**:
- 500 에러가 지속적으로 발생하는 경우
- 예상치 못한 OAuth2 설정 오류가 발생하는 경우
- 사용자 로그인이 불가능한 경우

**롤백 절차**:
```bash
# 이전 커밋으로 되돌리기
git revert <commit-hash>

# 또는 이전 배포로 롤백
kubectl rollout undo deployment/dpm-core-server
```

**Prod 배포 완료 조건**:
- **500 에러가 완전히 해결됨** ⭐
- Apple 및 Kakao 로그인 모두 정상 작동
- 에러 로그 없음
- 사용자 피드백 없음

---

## 자동 검증 명령어

### LOCAL 환경 전체 검증

```bash
# Phase 1: 설정 수정 검증
./gradlew :application:processResources
./gradlew :application:compileKotlin

# 애플리케이션 시작
./gradlew :application:bootRun --args='--spring.profiles.active=local' &
APP_PID=$!

# 시작 대기
sleep 15

# Phase 3: CURL 테스트
echo "=== Testing Swagger UI ==="
curl -I http://localhost:8080/swagger-ui/index.html

echo "=== Testing Kakao Login Redirect ==="
curl -v http://localhost:8080/login/kakao \
  -H "Origin: http://localhost:3000" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"

echo "=== Testing Apple Login Redirect ==="
curl -v http://localhost:8080/login/apple \
  -H "Origin: http://localhost:3000" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"

echo "=== Testing Health Check ==="
curl http://localhost:8080/actuator/health

# 애플리케이션 종료
kill $APP_PID 2>/dev/null
```

### Dev/Prod 환경 검증 (배포 후)

```bash
# Dev 환경
echo "=== Dev: Apple Login Redirect ==="
curl -v https://core.depromeet.shop/login/apple \
  -H "Origin: https://core.depromeet.shop" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"

echo "=== Dev: Kakao Login Redirect ==="
curl -v https://core.depromeet.shop/login/kakao \
  -H "Origin: https://core.depromeet.shop" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"

# Prod 환경
echo "=== Prod: Apple Login Redirect ==="
curl -v https://core.depromeet.com/login/apple \
  -H "Origin: https://core.depromeet.com" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"

echo "=== Prod: Kakao Login Redirect ==="
curl -v https://core.depromeet.com/login/kakao \
  -H "Origin: https://core.depromeet.com" \
  2>&1 | grep -E "(HTTP|Location|Set-Cookie)"
```

---

## 수동 테스트 체크리스트

### LOCAL 환경

- [ ] Swagger UI 접속 → "Member-Login" 태그 표시됨
- [ ] Swagger UI에서 `/login/kakao` 엔드포인트 표시됨
- [ ] Swagger UI에서 `/login/apple` 엔드포인트 표시됨
- [ ] GET `/login/kakao` → HTTP 302, `/oauth2/authorization/kakao` 리다이렉션
- [ ] GET `/login/apple` → HTTP 302, `/oauth2/authorization/apple` 리다이렉션
- [ ] GET `/login/apple` → **500 에러 없음** ✨
- [ ] GET `/oauth2/authorization/kakao` → Kakao 인증 페이지로 리다이렉션
- [ ] GET `/oauth2/authorization/apple` → Apple 인증 페이지로 리다이렉션
- [ ] Health Check → HTTP 200, `{"status":"UP"}`

### Dev 환경 (PR merge to develop 후)

- [ ] Dev 배포 성공
- [ ] Dev URL에서 `/login/apple` → HTTP 302, Apple OAuth2로 리다이렉션
- [ ] Dev URL에서 `/login/apple` → **500 에러 없음** ✨
- [ ] Dev URL에서 `/login/kakao` → HTTP 302, Kakao OAuth2로 리다이렉션
- [ ] Dev 환경 로그에 OAuth2 설정 병합 에러 없음

### Prod 환경 (PR merge to main 후)

- [ ] **Prod 배포 성공**
- [ ] **Prod URL에서 `/login/apple` → HTTP 302, Apple OAuth2로 리다이렉션** ⭐
- [ ] **Prod URL에서 `/login/apple` → 500 에러 없음** ⭐⭐⭐ **(최종 성공 지표)**
- [ ] Prod URL에서 `/login/kakao` → HTTP 302, Kakao OAuth2로 리다이렉션
- [ ] Prod 환경 로그에 에러 없음
- [ ] 사용자 피드백 모니터링

---

## 성공 기준

### 자동 검증 (LOCAL)

- [ ] `./gradlew :application:compileKotlin` 실행 시 BUILD SUCCESSFUL
- [ ] `./gradlew :application:build` 실행 시 BUILD SUCCESSFUL
- [ ] Swagger UI에서 모든 로그인 엔드포인트 표시됨
- [ ] `/login/apple` curl 테스트 → HTTP 302, 500 에러 없음

### 수동 테스트

- [ ] LOCAL 환경에서 Apple 및 Kakao 로그인 리다이렉션 모두 성공
- [ ] LOCAL 환경에서 **500 에러 없음** ✨
- [ ] Dev 환경에서 OAuth2 로그인 리다이렉션 성공 (merge 후)
- [ ] **Prod 환경에서 Apple 로그인 시 500 에러 발생하지 않음** ⭐⭐⭐
- [ ] Prod 환경에서 Kakao 로그인 정상 작동 (회귀 없음)

### 모니터링

- [ ] 배포 후 24시간 동안 OAuth2 관련 에러 로그 없음
- [ ] 사용자 로그인 성공률이 배포 전과 유지 또는 개선

---

## 실패 시 롤백 계획

### 롤백 기준

- 배포 후 1시간 이내에 500 에러가 10건 이상 발생
- OAuth2 로그인 성공률이 90% 미만으로 떨어지는 경우
- 예상치 못한 보안 이슈가 발생하는 경우

### 롤백 절차

1. 즉시 이전 안정 버전으로 재배포
2. 환경 변수를 이전 설정으로 복원
3. 에러 로그 분석 및 문제점 파악
4. 수정 후 재배포

---

## 위험 요소 및 완화 방안

### 위험 1: 환경 변수 누락

**위험도**: 높음
**영향**: 애플리케이션 시작 실패
**완화 방안**:
- 각 profile 배포 전에 환경 변수 체크리스트 확인
- CI/CD 파이프라인에서 필수 환경 변수 검증 스크립트 추가

### 위험 2: Apple Private Key 형식 오류

**위험도**: 중간
**영향**: 클라이언트 시크릿 생성 실패
**완화 방안**:
- Private key가 올바른 PEM 형식인지 사전 검증
- LOCAL 환경에서 먼저 검증

### 위험 3: @Controller Swagger 표시 문제

**위험도**: 낮음
**영향**: API 문서에 로그인 엔드포인트가 표시되지 않음
**완화 방안**:
- Phase 0에서 OpenAPI 설정으로 명시적으로 엔드포인트 추가
- Swagger UI에서 수동 테스트로 확인

### 위험 4: Prod 배포 후 회귀 발생

**위험도**: 높음
**영향**: Kakao 로그인도 실패할 수 있음
**완화 방안**:
- LOCAL 환경에서 철저히 curl 테스트
- Dev 환경에서 먼저 검증 (merge to develop)
- 롤백 계획 준비

---

## 참고 자료

### 내부 문서
- `2026-01-18-apple-oauth2-500-error-investigation.md` - 연구 문서

### 외부 참조
- [Apple Sign In for Web - Documentation](https://developer.apple.com/documentation/signinwithapple/configuring-your-webpage-for-sign-in-with-apple)
- [Spring Boot YAML Configuration Merge](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.files.yaml.merging)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login.html)
- [Springdoc OpenAPI Customization](https://springdoc.org/#how-can-i-define-and-group-my-controllers-and-operatings)

---

## 다음 단계

이 계획의 모든 단계가 완료되면:

1. **문서화**: 배포 과정과 학습점을 문서화
2. **모니터링**: 지속적인 OAuth2 로그인 성공률 모니터링
3. **개선**: 성능 최적화 (클라이언트 시크릿 캐싱 등)

---

## 계획 상태

- [x] 연구 단계 완료
- [x] Apple 표준 구현 방법 검토 완료
- [x] 계획 수정 (테스트 제외, curl 검증 추가, Swagger 문제 해결 포함)
- [ ] Phase 0: @Controller Swagger 표시 문제 해결
- [ ] Phase 1: 설정 수정 검증
- [ ] Phase 2: 환경 변수 설정 확인
- [ ] Phase 3: LOCAL 환경 CURL 테스트
- [ ] Phase 4: Dev 환경 배포 및 검증 (PR merge to develop 후)
- [ ] Phase 5: Prod 환경 배포 및 최종 검증 (PR merge to main 후)
