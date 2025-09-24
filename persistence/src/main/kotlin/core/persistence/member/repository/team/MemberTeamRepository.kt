package core.persistence.member.repository.team

import core.domain.member.aggregate.MemberTeam
import core.domain.member.port.outbound.MemberTeamPersistencePort
import jooq.dsl.tables.references.MEMBER_TEAMS
import org.jooq.DSLContext
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
