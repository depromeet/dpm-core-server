package core.application.security.oauth.service

import core.application.security.oauth.domain.CustomOAuth2User
import core.application.security.oauth.exception.UnsupportedOAuthProviderException
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.security.core.authority.SimpleGrantedAuthority
import core.application.security.oauth.apple.AppleIdTokenValidator
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class CustomOAuth2UserService(
    private val roleQueryUseCase: RoleQueryUseCase,
    private val appleIdTokenValidator: AppleIdTokenValidator,
) : DefaultOAuth2UserService() {
    /**
     * OAuth2UserRequest 객체에서 OAuth2User를 로드하고, 해당 사용자가 소유한 권한을 조회하여 CustomOAuth2User 객체로 반환함.
     * 사용자가 보유한 권한이 없으면 기본적으로 ROLE_GUEST 권한을 부여함.
     *
     * @throws OAuth2AuthenticationException
     *
     * @author LeeHanEum
     * @since 2025.07.12
     */
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val registrationId = userRequest.clientRegistration.registrationId.lowercase()

        val oAuth2User: OAuth2User = when (registrationId) {
            "apple" -> loadAppleUser(userRequest)
            else -> super.loadUser(userRequest)
        }

        val externalId = when (registrationId) {
            "apple" -> oAuth2User.attributes["sub"] as String
            else -> oAuth2User.name
        }

        val roles = roleQueryUseCase.getRolesByExternalId(externalId)
            .ifEmpty { listOf("GUEST") }

        val authorities = roles
            .map { SimpleGrantedAuthority("ROLE_$it") }
            .toSet()

        val nameAttributeKey = when (registrationId) {
            "apple" -> "sub"
            "kakao" -> "id"
            else -> userRequest.clientRegistration
                .providerDetails.userInfoEndpoint.userNameAttributeName
        }
        return CustomOAuth2User(
            authorities = authorities.toList(),
            attributes = oAuth2User.attributes,
            nameAttributeKey = nameAttributeKey,
            authAttributes = OAuthAttributes.of(
                registrationId.uppercase(),
                oAuth2User.attributes
            ) ?: throw UnsupportedOAuthProviderException(),
        )
    }

    private fun loadAppleUser(userRequest: OAuth2UserRequest): OAuth2User {
        val idToken = userRequest.additionalParameters["id_token"]
                as? String
            ?: throw OAuth2AuthenticationException("Apple id_token missing")

        val claims = appleIdTokenValidator.verify(idToken)

        return DefaultOAuth2User(
            emptySet(),
            claims,
            "sub"
        )
    }
}
