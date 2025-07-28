package com.server.dpmcore.security.oauth.service

import com.server.dpmcore.authority.domain.port.inbound.AuthorityQueryUseCase
import com.server.dpmcore.security.oauth.domain.CustomOAuth2User
import com.server.dpmcore.security.oauth.dto.OAuthAttributes
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class CustomOAuth2UserService(
    private val authorityQueryUseCase: AuthorityQueryUseCase,
) : DefaultOAuth2UserService() {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val clientRegistration = userRequest.clientRegistration
        val roleNames = authorityQueryUseCase.getAuthoritiesByMember(clientRegistration.registrationId)

        val grantedAuthorities = mutableSetOf<GrantedAuthority>()

        for (role in roleNames) {
            grantedAuthorities +=
                SimpleGrantedAuthority("ROLE_$role")
        }

        return CustomOAuth2User(
            authorities = grantedAuthorities.toList(),
            attributes = oAuth2User.attributes,
            nameAttributeKey = clientRegistration.providerDetails.userInfoEndpoint.userNameAttributeName,
            authAttributes = OAuthAttributes.of(clientRegistration.registrationId.uppercase(), oAuth2User.attributes),
        )
    }
}
