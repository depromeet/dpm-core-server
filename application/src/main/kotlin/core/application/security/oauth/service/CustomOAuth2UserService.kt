package core.application.security.oauth.service

import core.application.security.oauth.domain.CustomOAuth2User
import core.application.security.oauth.exception.UnsupportedOAuthProviderException
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class CustomOAuth2UserService(
    private val roleQueryUseCase: RoleQueryUseCase,
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
        val oAuth2User = super.loadUser(userRequest)
        val clientRegistration = userRequest.clientRegistration
        val roleNames = roleQueryUseCase.getRolesByExternalId(clientRegistration.registrationId)

        val grantedAuthorities = mutableSetOf<GrantedAuthority>()

        for (role in roleNames) {
            grantedAuthorities +=
                SimpleGrantedAuthority("ROLE_$role")
        }

        return CustomOAuth2User(
            authorities = grantedAuthorities.toList(),
            attributes = oAuth2User.attributes,
            nameAttributeKey = clientRegistration.providerDetails.userInfoEndpoint.userNameAttributeName,
            authAttributes =
                OAuthAttributes.of(clientRegistration.registrationId.uppercase(), oAuth2User.attributes)
                    ?: throw UnsupportedOAuthProviderException(),
        )
    }
}
