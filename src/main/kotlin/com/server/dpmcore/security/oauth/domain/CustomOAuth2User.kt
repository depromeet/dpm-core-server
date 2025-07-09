package com.server.dpmcore.security.oauth.domain

import com.server.dpmcore.security.oauth.dto.OAuthAttributes
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User

class CustomOAuth2User(
    authorities: Collection<GrantedAuthority>,
    attributes: Map<String, Any>,
    nameAttributeKey: String,
    val authAttributes: OAuthAttributes
) : DefaultOAuth2User(authorities, attributes, nameAttributeKey)
