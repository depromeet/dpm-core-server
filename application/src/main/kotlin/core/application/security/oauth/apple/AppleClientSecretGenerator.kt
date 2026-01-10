package core.application.security.oauth.apple

import core.application.security.properties.AppleProperties
import io.jsonwebtoken.Jwts
import jakarta.annotation.PostConstruct
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64
import java.util.Date
@Component
class AppleClientSecretGenerator(
    private val appleProperties: AppleProperties,
) {

    private lateinit var privateKey: PrivateKey

    @PostConstruct
    fun init() {
        privateKey = loadPrivateKeyOnce(appleProperties.privateKeyPath)
    }

    fun generateClientSecret(): String {
        val now = Date()
        val expiration = Date.from(
            LocalDateTime.now()
                .plusDays(180)
                .atZone(ZoneId.systemDefault())
                .toInstant()
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
            .signWith(privateKey) // ✅ 캐싱된 키
            .compact()
    }

    private fun loadPrivateKeyOnce(resource: Resource): PrivateKey {
        val keyContent = resource.inputStream.bufferedReader().use { it.readText() }
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        val decoded = Base64.getDecoder().decode(keyContent)
        val keySpec = PKCS8EncodedKeySpec(decoded)
        return KeyFactory.getInstance("EC").generatePrivate(keySpec)
    }
}
