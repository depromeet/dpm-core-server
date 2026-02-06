---
date: 2026-01-18T10:45:00+09:00
researcher: claude
git_commit: 61cebdf
branch: refactor/#232
repository: dpm-core-server
topic: "Apple OAuth 2.0 500 Error Root Cause Analysis and Solution"
tags: [research, oauth2, apple, security, bugfix]
status: complete
last_updated: 2026-01-18
last_updated_by: claude
---

# 연구: Apple OAuth 2.0 500 에러 원인 분석 및 해결 방안

**날짜**: 2026-01-18 10:45:00 KST
**연구자**: claude
**Git 커밋**: 61cebdf
**브랜치**: refactor/#232
**저장소**: dpm-core-server

## 연구 질문

현재 프로젝트에서 Apple OAuth 2.0 기능을 지원하는데, prod 환경에서 navigation 시 500 에러가 발생합니다. Kakao 로그인과 함께 지원하면서 local, dev, prod 모든 환경에서 문제가 발생하는 근본 원인을 분석하고 개선 방안을 연구합니다.

## 요약

### 핵심 발견

1. **설정 병합 문제**: Spring Boot의 YAML 설정 병합 메커니즘이 OAuth2 client 설정에서 예상대로 작동하지 않음
2. **설정 구조 불일치**: 기본 `application.yml`에는 Kakao만 존재하고 Apple은 profile별 파일에만 존재
3. **Spring Security 제약**: `spring.security.oauth2.client` 키는 중복 정의 시 전체가 덮어쓰여짐

### 500 에러의 근본 원인

**Primary Issue**: Spring Boot OAuth2 Client Registration 병합 실패

`application.yml` (기본 설정) 구조:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao: [Kakao 설정]
        provider:
          kakao: [Kakao provider 설정]
```

Profile별 파일(`application-prod.yml`, `application-dev.yml`, `application-local.yml`) 구조:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          apple: [Apple 설정]
        provider:
          apple: [Apple provider 설정]
```

**문제**: Spring Boot가 YAML을 병합할 때 `oauth2.client` 레벨에서 전체 구조가 덮어씌워져서 Kakao 설정이 손실됨

### Dev 환경에서 문제가 없었던 이유

**가능한 시나리오**:
1. Dev 배포 시점에는 기본 `application.yml`에 Apple 설정이 없었을 가능성
2. 또는 Dev 배포 이후에 기본 `application.yml`에 Kakao 설정이 추가/수정되었을 가능성
3. 커밋 히스토리를 보면 `[refactor]: add cd apple parameter pipeline of prod` 커밋이 존재 → prod 전용 설정 추가

**추정**: Dev 환경 배포 당시에는 profile 파일이 병합되어 정상 작동했으나, 기본 `application.yml` 구조 변경 후 prod 배포에서 문제 발생

## 상세 분석

### 프로젝트 구조

**Multi-module Gradle 프로젝트**:
- `entity/`: JPA 엔티티 (MemberEntity, MemberOAuthEntity)
- `domain/`: 도메인 로직 및 포트 (OAuthProvider, OAuthAttributes)
- `persistence/`: JPA 리포지토리 구현
- `application/`: 컨트롤러, 서비스, 설정

**주요 OAuth 관련 파일**:
- `CustomOAuth2UserService.kt`: OAuth2 사용자 로드 핵심 로직
- `CustomOAuth2AccessTokenResponseClient.kt`: Apple 클라이언트 시크릿 동적 생성
- `AppleClientSecretGenerator.kt`: JWT 기반 Apple 시크릿 생성
- `AppleIdTokenValidator.kt`: Apple ID 토큰 검증
- `SecurityConfig.kt`: Spring Security 설정

### Apple OAuth2 구현 분석

#### 1. CustomOAuth2UserService (`application/src/main/kotlin/core/application/security/oauth/service/CustomOAuth2UserService.kt:30-59`)

```kotlin
override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
    val registrationId = userRequest.clientRegistration.registrationId.lowercase()

    val oAuth2User: OAuth2User = when (registrationId) {
        "apple" -> loadAppleUser(userRequest)  // Apple 별도 처리
        else -> super.loadUser(userRequest)      // Kakao 등 기본 처리
    }
    // ...
}

private fun loadAppleUser(userRequest: OAuth2UserRequest): OAuth2User {
    val idToken = userRequest.additionalParameters["id_token"]
            as? String
        ?: throw OAuth2AuthenticationException("Apple id_token missing")

    val claims = appleIdTokenValidator.verify(idToken)
    // ...
}
```

**동작 방식**:
- Apple일 때: `id_token`을 추출해서 검증
- Kakao일 때: 기본 OAuth2UserRequest 처리

#### 2. CustomOAuth2AccessTokenResponseClient (`application/src/main/kotlin/core/application/security/oauth/client/CustomOAuth2AccessTokenResponseClient.kt:18-49`)

```kotlin
override fun getTokenResponse(authorizationGrantRequest: OAuth2AuthorizationCodeGrantRequest): OAuth2AccessTokenResponse {
    val clientRegistration = authorizationGrantRequest.clientRegistration

    if (clientRegistration.registrationId.equals("apple", ignoreCase = true)) {
        return getAppleTokenResponse(authorizationGrantRequest, clientRegistration)
    }

    return defaultClient.getTokenResponse(authorizationGrantRequest)
}

private fun getAppleTokenResponse(...): OAuth2AccessTokenResponse {
    val secret = appleClientSecretGenerator.generateClientSecret()  // 동적 시크릿 생성

    val newClientRegistration = ClientRegistration
        .withClientRegistration(registration)
        .clientSecret(secret)  // 동적 시크릿 주입
        .build()
    // ...
}
```

**핵심**: Apple은 JWT 기반 클라이언트 시크릿이 필요해서 동적으로 생성

### Kakao OAuth 구현 분석

#### KakaoAuthAttributes (`domain/src/main/kotlin/core/domain/security/oauth/dto/KakaoAuthAttributes.kt:19-32`)

```kotlin
fun of(attributes: Map<String, Any>): KakaoAuthAttributes {
    val externalId = attributes["id"] as Long
    val kakaoAccount = attributes["kakao_account"] as Map<*, *>
    val email = kakaoAccount["email"] as String

    return KakaoAuthAttributes(
        externalId = externalId.toString(),
        email = email,
        name = kakaoAccount["profile"]?.let { (it as Map<*, *>)["nickname"] as String } ?: "",
        provider = OAuthProvider.KAKAO,
    )
}
```

**동작 방식**: Kakao API 응답 구조에서 필요한 정보 추출

### 설정 파일 구조 문제

#### 현재 구조 (문제 있음)

**application.yml** (기본):
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao: [...]  # Kakao만 있음
        provider:
          kakao: [...]
```

**application-{profile}.yml**:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          apple: [...]  # Apple만 추가
        provider:
          apple: [...]
```

**Spring Boot 병합 결과**:
- `oauth2.client` 레벨에서 전체 교체 발생
- Kakao 설정이 profile별 설정에 의해 덮어씌워짐
- 결과: Kakao도 손실되고 Apple도 제대로 로드되지 않음

### 500 에러 발생 시나리오

1. **사용자가 /oauth2/authorization/apple 또는 /oauth2/authorization/kakao로 접근**
2. **Spring Security가 ClientRegistration을 로드하려고 시도**
3. **설정 병합 실패로 인해 ClientRegistration이 제대로 생성되지 않음**
4. **CustomOAuth2UserService.loadUser() 호출 시 userRequest가 잘못됨**
5. **loadAppleUser()에서 id_token 누락 → OAuth2AuthenticationException 발생**
6. **또는 기본 super.loadUser(userRequest) 실패**

## 코드 참조

### OAuth2 설정 구조

- `application/src/main/resources/application.yml:37-54` - 기본 설정 (Kakao만 있음)
- `application/src/main/resources/application-dev.yml:10-28` - Dev 환경 Apple 설정
- `application/src/main/resources/application-prod.yml:10-28` - Prod 환경 Apple 설정
- `application/src/main/resources/application-local.yml:10-28` - Local 환경 Apple 설정

### OAuth2 핵심 구현

- `application/src/main/kotlin/core/application/security/oauth/service/CustomOAuth2UserService.kt:30-59` - OAuth2 사용자 로드
- `application/src/main/kotlin/core/application/security/oauth/client/CustomOAuth2AccessTokenResponseClient.kt:18-49` - Apple 토큰 교환
- `application/src/main/kotlin/core/application/security/oauth/apple/AppleClientSecretGenerator.kt:28-54` - 클라이언트 시크릿 생성
- `application/src/main/kotlin/core/application/security/configuration/SecurityConfig.kt:80-95` - OAuth2 로그인 설정

### 도메인 모델

- `domain/src/main/kotlin/core/domain/security/oauth/dto/OAuthAttributes.kt:14-24` - OAuth 속성 팩토리
- `domain/src/main/kotlin/core/domain/security/oauth/dto/AppleAuthAttributes.kt:5-19` - Apple 속성
- `domain/src/main/kotlin/core/domain/security/oauth/dto/KakaoAuthAttributes.kt:12-42` - Kakao 속성

## 해결 방안

### 권장 해결책: 기본 설정에서 Apple 제거 및 각 Profile에 완전한 설정 추가

**application.yml**:
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

**application-{profile}.yml** (dev, local, prod):
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          apple:
            client-id: ${PROFILE_APPLE_CLIENT_ID}
            client-secret: ${PROFILE_APPLE_CLIENT_SECRET}
            redirect-uri: ${PROFILE_APPLE_REDIRECT_URI}
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
  client-id: ${PROFILE_APPLE_CLIENT_ID}
  key-id: ${PROFILE_APPLE_KEY_ID}
  team-id: ${PROFILE_APPLE_TEAM_ID}
  private-key: ${PROFILE_APPLE_PRIVATE_KEY}
  redirect-uri: ${PROFILE_APPLE_REDIRECT_URI}
```

**변경 사항**:
- 기본 `application.yml`에서 Apple 설정 완전 제거
- 각 profile 파일에 Apple의 **완전한** 설정 포함
- Kakao는 기본 설정에 유지

### 대안 2: Spring Boot 2.4+ 설정 가져오기 사용

```yaml
# application.yml
spring:
  config:
    import:
      - classpath:oauth2-kakao.yml

# application-{profile}.yml
spring:
  config:
    import:
      - classpath:oauth2-apple.yml
```

하지만 현재 프로젝트 구조에서는 권장 해결책이 더 간단하고 명확함.

### 대안 3: 환경 변수로 통합

모든 설정을 환경 변수로 관리하고 profile 간 중복 제거.

**장점**:
- YAML 병합 문제 완전 회피
- 설정 중복 제거

**단점**:
- 환경 변수 관리 복잡성 증가
- `AppleProperties` 같은 커스텀 설정 처리 필요

## 구현 가이드

### 단계별 적용 방법

1. **application.yml 수정**:
   ```bash
   # 기본 설정에서 Apple 관련 부분 제거 유지
   # (현재 상태 유지 - Kakao만 있음)
   ```

2. **각 profile 파일에 완전한 Apple 설정 추가**:
   - 이미 완료됨 (이전 작업으로 이미 수정됨)
   - `application-local.yml`, `application-dev.yml`, `application-prod.yml` 확인

3. **검증**:
   ```bash
   # 설정 로드 테스트
   ./gradlew :application:bootRun --args='--spring.profiles.active=local'

   # 빌드 및 리소스 처리 확인
   ./gradlew :application:processResources
   ```

4. **배포 전 테스트**:
   - Local 환경에서 Apple & Kakao 로그인 테스트
   - Dev 환경에 배포 후 OAuth2 플로우 확인
   - Prod 배포 전 전체 OAuth2 플로우 검증

## 테스트 전략

### 단위 테스트

**AppleClientSecretGeneratorTest** (`application/src/test/kotlin/core/application/security/oauth/apple/AppleClientSecretGeneratorTest.kt`):
- 클라이언트 시크릿 생성 로직 검증
- JWT 서명 검증

**CustomOAuth2UserService 통합 테스트**:
- Mock OAuth2UserRequest로 Apple & Kakao 분기 테스트
- loadAppleUser의 id_token 추출 로직 검증

### 통합 테스트

**OAuth2 플로우 테스트**:
1. 인증 요청 (`/oauth2/authorization/{provider}`)
2. 콜백 처리 (`/login/oauth2/code/*`)
3. 토큰 발급 및 리다이렉션

**TestContainers를 활용한 통합 테스트**:
```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class OAuth2IntegrationTest {
    @Test
    fun `Apple login should work end-to-end`() {
        // Apple 모의 OAuth2 서버 사용
    }

    @Test
    fun `Kakao login should work end-to-end`() {
        // Kakao 모의 OAuth2 서버 사용
    }
}
```

### 수동 테스트 체크리스트

**Local 환경**:
- [ ] Apple 로그인 버튼 클릭 → Apple 로그인 페이지 리다이렉션
- [ ] Apple 인증 완료 후 콜백 수신
- [ ] JWT 토큰 발급 및 쿠키 설정 확인
- [ ] Kakao 로그인 동일 플로우 확인

**Dev 환경**:
- [ ] Dev 배포 후 동일한 플로우 검증
- [ ] 로그에서 OAuth2 설정 로드 확인

**Prod 환경**:
- [ ] 500 에러 없는지 확인
- [ ] 모든 브라우저에서 테스트
- [ ] 에러 로그 모니터링

## 주의사항 및 리스크

### 1. 환경 변수 누락

**리스크**: 필수 환경 변수가 설정되지 않으면 애플리케이션 시작 실패

**완화**:
```yaml
client-id: ${PROFILE_APPLE_CLIENT_ID:}  # 빈값 허용
```

또는 `@ConfigurationProperties` 검증 추가

### 2. Apple Private Key 형식

**리스크**: Private key가 올바른 형식이 아닌 경우 `AppleClientSecretGenerator` 초기화 실패

**완화**:
```kotlin
@PostConstruct
fun init() {
    try {
        privateKey = getPrivateKey(appleProperties.privateKey)!!
    } catch (e: Exception) {
        log.error("Failed to initialize Apple private key", e)
        throw e
    }
}
```

### 3. OAuth2 콜백 URL 불일치

**리스크**: 각 환경의 redirect-uri가 실제 배포 환경과 불일치

**완화**:
- 각 profile의 redirect-uri를 환경 변수로 관리
- 배포 시점에 값 검증

### 4. Kakao와 Apple 동시 사용 시 충돌

**리스크**: CustomOAuth2UserService에서 분기 로직 오류

**완화**:
- 이미 구현되어 있음 (`when` 문으로 분기)
- 테스트로 양쪽 모두 검증

## 성능 고려사항

### 1. Apple Public Key 캐싱

**현재**: `AppleIdTokenValidator`에서 `ConcurrentHashMap`으로 캐싱
**개선 여지**: 캐시 만료 정책 추가 (현재 없음)

```kotlin
private val applePublicKeys = ConcurrentHashMap<String, PublicKey>()
private val cacheExpiry = ConcurrentHashMap<String, Long>()

fun getPublicKey(kid: String): PublicKey {
    val now = System.currentTimeMillis()
    if (cacheExpiry[kid]?.let { it > now } == true) {
        return applePublicKeys[kid]!!
    }
    // 재조회
}
```

### 2. JWT 생성 비용

**현재**: 모든 Apple 로그인 시마다 클라이언트 시크릿 생성
**개선 여지**: 클라이언트 시크릿 캐싱 (만료 6개월이므로 가능)

```kotlin
private var cachedClientSecret: String? = null
private var cacheExpiry: Long = 0

fun generateClientSecret(): String {
    val now = System.currentTimeMillis()
    if (cacheExpiry > now && cachedClientSecret != null) {
        return cachedClientSecret!!
    }
    // 재생성
    val secret = Jwts.builder()...
    cachedClientSecret = secret
    cacheExpiry = now + (5 * 60 * 1000) // 5분 캐시
    return secret
}
```

## 보안 고려사항

### 1. Client Secret 관리

**현재**: 환경 변수로 관리 (`APPLE_PRIVATE_KEY`)
**권장사항**:
- Prod에서는 secret management 서비스 사용 (AWS Secrets Manager 등)
- Private key는 파일 시스템에서 읽기 (권한 제어)

### 2. ID Token 검증

**현재**: Apple 공개키로 서명 검증 (`AppleIdTokenValidator`)
**검증 항목**:
- Issuer: `https://appleid.apple.com` ✓
- Audience: `appleProperties.clientId` ✓
- 만료: JWT 라이브러리가 자동 검증 ✓

### 3. CSRF 방어

**현재**: CSRF 비활성화 (`SecurityConfig.kt:47`)
**권장사항**: OAuth2 로그인에 대한 CSRF는 Spring Security가 기본 처리

## 참고 자료

### 내부 문서
- 없음 (최초 연구 문서)

### 외부 참조
- [Spring Boot YAML Configuration Merge](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.files.yaml.merging)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Apple Sign In JS Documentation](https://developer.apple.com/sign-in-with-apple/)
- [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login.html)

## 관련 커밋

- `61cebdf` - [refactor]: add cd apple parameter pipeline of prod
- `d1d9801` - Merge branch 'main' into develop

## 열린 질문

1. **Dev 환경에서 실제로 Apple 로그인이 정상 작동했는지?**
   - 사용자가 테스트했다고 하지만 실제로는 다른 profile이 사용되었을 가능성
   - 또는 Dev 배포 시점에는 기본 application.yml 구조가 달랐을 가능성

2. **테스트 컴파일 오류 해결 필요**
   - `AppleAuthServiceTest.kt`의 생성자 인자 순서 불일치
   - `AppleOAuth2AccessTokenResponseClientTest.kt`의 클래스명 참조 오류

3. **모든 환경의 환경 변수가 올바르게 설정되었는지?**
   - 각 profile에 맞는 환경 변수 확인 필요

## 다음 단계

1. **설정 수정 검증**: 이미 완료된 수정 사항 확인
2. **테스트 오류 수정**: 컴파일 오류 해결
3. **Local 환경 테스트**: Apple & Kakao 로그인 플로우 검증
4. **Dev 배포 및 테스트**: Dev 환경 배포 후 OAuth2 동작 확인
5. **Prod 배포**: 500 에러 해결 확인
