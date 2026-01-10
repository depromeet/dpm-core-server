package core.application.security.oauth.apple

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import core.application.security.properties.AppleProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

@Component
class AppleIdTokenValidator(
    private val appleProperties: AppleProperties,
) {
    private val restClient = RestClient.create()
    private val objectMapper = jacksonObjectMapper()
    private val applePublicKeys = ConcurrentHashMap<String, PublicKey>()

    fun verify(idToken: String): Claims {
        // 1. Get KID from Header
        val header = getIdTokenHeader(idToken)
        val kid = header["kid"] as? String ?: throw IllegalArgumentException("Invalid ID Token: kid missing")

        // 2. Get Public Key (Cache or Fetch)
        val publicKey = getPublicKey(kid)

        // 3. Verify Signature & Claims
        return Jwts.parser()
            .verifyWith(publicKey)
            .requireIssuer("https://appleid.apple.com")
            .requireAudience(appleProperties.clientId)
            .build()
            .parseSignedClaims(idToken)
            .payload
    }

    private fun getIdTokenHeader(idToken: String): Map<String, Any> {
        val parts = idToken.split(".")
        if (parts.size < 2) throw IllegalArgumentException("Invalid ID Token format")
        val headerJson = String(Base64.getUrlDecoder().decode(parts[0]))
        return objectMapper.readValue(headerJson)
    }

    private fun getPublicKey(kid: String): PublicKey {
        return applePublicKeys[kid] ?: fetchAndCachePublicKeys()[kid]
        ?: throw IllegalArgumentException("Matching key not found for kid: $kid")
    }

    private fun fetchAndCachePublicKeys(): Map<String, PublicKey> {
        val response = restClient.get()
            .uri("https://appleid.apple.com/auth/keys")
            .retrieve()
            .body(String::class.java)
            ?: throw RuntimeException("Failed to fetch Apple public keys")

        val keysDef = objectMapper.readValue<AppleKeysResponse>(response)
        val newKeys = keysDef.keys.associate { key ->
            key.kid to generatePublicKey(key)
        }
        
        applePublicKeys.putAll(newKeys)
        return newKeys
    }

    private fun generatePublicKey(keyDef: AppleKey): PublicKey {
        val nBytes = Base64.getUrlDecoder().decode(keyDef.n)
        val eBytes = Base64.getUrlDecoder().decode(keyDef.e)

        val n = BigInteger(1, nBytes)
        val e = BigInteger(1, eBytes)

        val spec = RSAPublicKeySpec(n, e)
        return KeyFactory.getInstance(keyDef.kty).generatePublic(spec)
    }

    data class AppleKeysResponse(
        val keys: List<AppleKey>
    )

    data class AppleKey(
        val kty: String,
        val kid: String,
        val use: String,
        val alg: String,
        val n: String,
        val e: String
    )
}
