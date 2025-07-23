package com.server.dpmcore.member.memberOAuth.domain.port

import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.memberOAuth.domain.model.MemberOAuth

interface MemberOAuthPersistencePort {
    fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    )
}
