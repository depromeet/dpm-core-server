package com.server.dpmcore.security.oauth.token

import com.server.dpmcore.security.properties.TokenProperties
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
) {
    fun generateAccessToken(externalId: String): String {
        return generateToken(externalId, tokenProperties.expirationTime.accessToken)
    }

    fun generateRefreshToken(externalId: String): String {
        return generateToken(externalId, tokenProperties.expirationTime.refreshToken)
    }

    fun generateToken(externalId: String, expirationTime: Long): String {
        val currentTimeMillis = System.currentTimeMillis()
        val now = Date(currentTimeMillis)
        val expiration = Date(currentTimeMillis + expirationTime * 1000)
        val secretKey = getSigningKey()

        return Jwts.builder()
            .subject(externalId)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    fun getAuthentication(token: String?): Authentication {
        val claims = getClaims(token)
        val authorities = setOf(SimpleGrantedAuthority("ROLE_USER"))

        return UsernamePasswordAuthenticationToken(
            User(claims.subject, "", authorities),
            token,
            authorities
        )
    }

    fun validateToken(token: String?): Boolean {
        return try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getClaims(token: String?): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(tokenProperties.secretKey)
        return Keys.hmacShaKeyFor(keyBytes)
    }
}
