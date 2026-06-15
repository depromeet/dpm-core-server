package com.server.dpmcore.member.memberOAuth.domain.port

import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.memberOAuth.domain.model.MemberOAuth
import com.server.dpmcore.member.memberOAuth.domain.model.OAuthProvider
import com.server.dpmcore.member.member.domain.model.MemberId

interface MemberOAuthPersistencePort {
    fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    )

    fun findMemberIdsByProvider(provider: OAuthProvider): List<MemberId>
}
