package core.application.security.oauth.token

import core.application.security.properties.TokenProperties
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.member.vo.MemberId
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val tokenProperties: TokenProperties,
    private val roleQueryUseCase: RoleQueryUseCase,
) {
    fun generateAccessToken(memberId: String): String =
        generateToken(memberId, tokenProperties.expirationTime.accessToken)

    fun generateRefreshToken(memberId: String): String =
        generateToken(memberId, tokenProperties.expirationTime.refreshToken)

    fun generateToken(
        memberId: String,
        expirationTime: Long,
    ): String {
        val currentTimeMillis = System.currentTimeMillis()
        val now = Date(currentTimeMillis)
        val expiration = Date(currentTimeMillis + expirationTime * 1000)
        val secretKey = getSigningKey()

        return Jwts
            .builder()
            .subject(memberId)
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
}
