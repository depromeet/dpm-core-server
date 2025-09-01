package com.server.dpmcore.member.memberTeam.infrastructure.repository

import com.server.dpmcore.member.memberTeam.domain.model.MemberTeam
import com.server.dpmcore.member.memberTeam.domain.port.outbound.MemberTeamPersistencePort
import org.jooq.DSLContext
import org.jooq.generated.tables.references.MEMBER_TEAMS
import org.springframework.stereotype.Repository

@Repository
class MemberTeamRepository(
    private val memberTeamJpaRepository: MemberTeamJpaRepository,
    private val dsl: DSLContext,
) : MemberTeamPersistencePort {
    override fun save(memberTeam: MemberTeam) {
        dsl
            .insertInto(MEMBER_TEAMS)
            .set(MEMBER_TEAMS.MEMBER_ID, memberTeam.memberId.value)
            .set(MEMBER_TEAMS.TEAM_ID, memberTeam.teamId.value)
            .execute()
    }

    override fun deleteByMemberId(memberId: Long) = memberTeamJpaRepository.deleteByMemberId(memberId)
}
