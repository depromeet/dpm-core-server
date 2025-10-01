package core.domain.member.port.outbound

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth

interface MemberOAuthPersistencePort {
    fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    )
}
