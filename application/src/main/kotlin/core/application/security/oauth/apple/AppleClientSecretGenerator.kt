package core.application.security.oauth.apple

import core.application.security.properties.AppleProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date


@Component
class AppleClientSecretGenerator(
    private val appleProperties: AppleProperties,
) {

    private lateinit var privateKey: PrivateKey

    @PostConstruct
    fun init() {
        privateKey = getPrivateKey(appleProperties.privateKey)!!
    }

    fun generateClientSecret(): String {
        val now = Date()
        val expiration = Date.from(
            LocalDateTime.now()
                .plusDays(180)
                .atZone(ZoneId.systemDefault())
                .toInstant(),
        )

        return Jwts.builder()
            .header()
            .add("kid", appleProperties.keyId)
            .and()
            .issuer(appleProperties.teamId)
            .subject(appleProperties.clientId)
            .audience().add("https://appleid.apple.com").and()
            .issuedAt(now)
            .expiration(expiration)
            .signWith(privateKey)
            .compact()
    }

    private fun getPrivateKey(privateKey: String): PrivateKey? {
        try {
            val replacedKey = privateKey.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
            // 1. Base64 디코딩
            val privateKeyBytes = Decoders.BASE64.decode(replacedKey)

            // 2. PKCS#8 스펙으로 변환
            val keySpec =
                PKCS8EncodedKeySpec(privateKeyBytes)
            val keyFactory = KeyFactory.getInstance("EC")

            // 4. PrivateKey 객체 생성
            return keyFactory.generatePrivate(keySpec)
        } catch (e: Exception) {
            throw RuntimeException("Base64 decoding failed", e)
        }
    }


}
