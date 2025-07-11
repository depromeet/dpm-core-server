package com.server.dpmcore.security.oauth.handler

import com.server.dpmcore.security.oauth.domain.CustomOAuth2User
import com.server.dpmcore.security.oauth.dto.OAuthAttributes
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class CustomOAuth2UserService : DefaultOAuth2UserService() {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val clientRegistration = userRequest.clientRegistration

        return CustomOAuth2User(
            authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes = oAuth2User.attributes,
            nameAttributeKey = clientRegistration.providerDetails.userInfoEndpoint.userNameAttributeName,
            authAttributes = OAuthAttributes.of(clientRegistration.registrationId.uppercase(), oAuth2User.attributes),
        )
    }
}
