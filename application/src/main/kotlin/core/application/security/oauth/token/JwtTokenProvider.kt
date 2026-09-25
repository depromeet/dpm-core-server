package core.application.security.oauth.token

import core.application.security.properties.TokenProperties
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.member.enums.LoginMethod
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val tokenProperties: TokenProperties,
    private val roleQueryUseCase: RoleQueryUseCase,
) {
    fun generateAccessToken(
        memberId: String,
        loginIdentity: LoginIdentity?,
    ): String = generateToken(memberId, tokenProperties.expirationTime.accessToken, loginIdentity)

    fun generateAccessTokenWithPermissions(
        memberId: String,
        permissions: List<SimpleGrantedAuthority>,
        loginIdentity: LoginIdentity?,
    ): String {
        val currentTimeMillis = System.currentTimeMillis()
        val now = Date(currentTimeMillis)
        val expiration = Date(currentTimeMillis + tokenProperties.expirationTime.accessToken * 1000)
        val secretKey = getSigningKey()

        return Jwts
            .builder()
            .subject(memberId)
            .claim("permissions", permissions.map { it.authority }) // Store permissions in token
            .withLoginIdentity(loginIdentity)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    /**
     * 리프레시 토큰은 매번 서로 다른 값이어야 한다.
     *
     * subject 와 초 단위 iat/exp 만으로 서명하면 같은 초에 같은 회원에게 발급한 두 토큰이
     * 바이트 단위로 동일해진다. 그러면 회전이 이전 토큰과 같은 token_hash 를 만들어
     * uk_rt_token_hash 유니크 제약을 위반하고, 재사용 탐지도 새 토큰을 회전된 토큰으로 오인한다.
     * jti 로 발급 건마다 고유성을 준다.
     *
     * 웹 OAuth 로그인은 리프레시 토큰만 내려주고 액세스 토큰은 재발급으로 받는다.
     * 그래서 로그인 계정을 리프레시 토큰에도 담아 두어야 재발급 이후까지 이어진다.
     */
    fun generateRefreshToken(
        memberId: String,
        loginIdentity: LoginIdentity?,
    ): String {
        val currentTimeMillis = System.currentTimeMillis()
        val now = Date(currentTimeMillis)
        val expiration = Date(currentTimeMillis + tokenProperties.expirationTime.refreshToken * 1000)

        return Jwts
            .builder()
            .id(UUID.randomUUID().toString())
            .subject(memberId)
            .withLoginIdentity(loginIdentity)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(getSigningKey())
            .compact()
    }

    fun generateToken(
        memberId: String,
        expirationTime: Long,
        loginIdentity: LoginIdentity?,
    ): String {
        val currentTimeMillis = System.currentTimeMillis()
        val now = Date(currentTimeMillis)
        val expiration = Date(currentTimeMillis + expirationTime * 1000)
        val secretKey = getSigningKey()

        return Jwts
            .builder()
            .subject(memberId)
            .withLoginIdentity(loginIdentity)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    fun getAuthentication(token: String?): Authentication {
        val claims = getClaims(token)
        val permissions =
            roleQueryUseCase
                .getPermissionsByMemberId(MemberId(claims.subject.toLong()))
                .map { SimpleGrantedAuthority(it) }

        return UsernamePasswordAuthenticationToken(
            User(claims.subject, "", permissions),
            token,
            permissions,
        )
    }

    fun validateToken(token: String?): Boolean =
        try {
            Jwts
                .parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }

    fun getMemberId(token: String?): Long {
        val claims = getClaims(token)
        return claims.subject.toLong()
    }

    /** 로그인 계정 클레임이 없는 토큰(배포 전 발급분)이면 null 을 반환한다. */
    fun getLoginIdentity(token: String?): LoginIdentity? {
        val claims = getClaims(token)
        val method = LoginMethod.fromOrNull(claims[LOGIN_METHOD_CLAIM] as? String) ?: return null
        val accountId = (claims[LOGIN_ACCOUNT_ID_CLAIM] as? Number)?.toLong() ?: return null
        return LoginIdentity(method, accountId)
    }

    private fun JwtBuilder.withLoginIdentity(loginIdentity: LoginIdentity?): JwtBuilder =
        apply {
            loginIdentity?.let {
                claim(LOGIN_METHOD_CLAIM, it.method.name)
                claim(LOGIN_ACCOUNT_ID_CLAIM, it.accountId)
            }
        }

    private fun getClaims(token: String?): Claims =
        Jwts
            .parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload

    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(tokenProperties.secretKey)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    companion object {
        private const val LOGIN_METHOD_CLAIM = "loginMethod"
        private const val LOGIN_ACCOUNT_ID_CLAIM = "loginAccountId"
    }
}
