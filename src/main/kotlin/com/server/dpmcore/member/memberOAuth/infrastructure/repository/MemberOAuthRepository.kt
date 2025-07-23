package com.server.dpmcore.member.memberOAuth.infrastructure.repository

import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.memberOAuth.domain.model.MemberOAuth
import com.server.dpmcore.member.memberOAuth.domain.port.MemberOAuthPersistencePort
import com.server.dpmcore.member.memberOAuth.infrastructure.entity.MemberOAuthEntity
import org.springframework.stereotype.Repository

@Repository
class MemberOAuthRepository(
    private val memberOAuthJpaRepository: MemberOAuthJpaRepository,
) : MemberOAuthPersistencePort {
    override fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    ) {
        memberOAuthJpaRepository.save(MemberOAuthEntity.of(memberOAuth, member))
    }
}
