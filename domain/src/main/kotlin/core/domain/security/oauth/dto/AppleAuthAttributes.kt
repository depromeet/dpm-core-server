package core.domain.security.oauth.dto

import core.domain.member.enums.OAuthProvider

class AppleAuthAttributes(
    private val attributes: Map<String, Any>,
) : OAuthAttributes {
    override fun getExternalId(): String = attributes["sub"] as String

    override fun getProvider(): OAuthProvider = OAuthProvider.APPLE

    override fun getEmail(): String = attributes["email"] as? String ?: ""

    override fun getName(): String =
        "Apple User" // Apple ID Token usually doesn't have name unless requested specifically in first login scope

    companion object {
        fun of(attributes: Map<String, Any>): AppleAuthAttributes = AppleAuthAttributes(attributes)
    }
}
