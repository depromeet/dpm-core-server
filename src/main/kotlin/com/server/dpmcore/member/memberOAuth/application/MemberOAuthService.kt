package com.server.dpmcore.member.memberOAuth.application

import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.memberOAuth.domain.model.MemberOAuth
import com.server.dpmcore.member.memberOAuth.domain.port.MemberOAuthPersistencePort
import com.server.dpmcore.security.oauth.dto.OAuthAttributes
import org.springframework.stereotype.Service

@Service
class MemberOAuthService(
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
) {
    fun addMemberOAuthProvider(
        member: Member,
        authAttribute: OAuthAttributes,
    ) {
        memberOAuthPersistencePort.save(
            MemberOAuth.of(
                authAttribute.getExternalId(),
                authAttribute.getProvider(),
                member.id!!,
            ),
            member,
        )
    }
}
