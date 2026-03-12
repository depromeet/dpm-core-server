package core.persistence.member.repository.team

import core.domain.member.aggregate.MemberTeam
import core.domain.member.port.outbound.MemberTeamPersistencePort
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBER_TEAMS
import org.springframework.stereotype.Repository

@Repository
class MemberTeamRepository(
    private val memberTeamJpaRepository: MemberTeamJpaRepository,
    private val dsl: DSLContext,
) : MemberTeamPersistencePort {
    override fun save(memberTeam: MemberTeam) {
        // Keep a single active team row per member to avoid duplicate assignments.
        deleteByMemberId(memberTeam.memberId.value)

        dsl
            .insertInto(MEMBER_TEAMS)
            .set(MEMBER_TEAMS.MEMBER_ID, memberTeam.memberId.value)
            .set(MEMBER_TEAMS.TEAM_ID, memberTeam.teamId.value)
            .execute()
    }

    override fun deleteByMemberId(memberId: Long) = memberTeamJpaRepository.deleteByMemberId(memberId)
}
