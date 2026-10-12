package core.persistence.member.repository.team

import core.domain.member.aggregate.MemberTeam
import core.domain.member.port.outbound.MemberTeamPersistencePort
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBER_TEAMS
import org.jooq.dsl.tables.references.TEAMS
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

    override fun replaceCurrentCohortTeams(
        memberIds: List<Long>,
        cohortId: Long,
        teamId: Long?,
    ): Set<Long> {
        if (memberIds.isEmpty()) return emptySet()
        val existing =
            dsl.select(MEMBER_TEAMS.MEMBER_ID, MEMBER_TEAMS.TEAM_ID)
                .from(MEMBER_TEAMS)
                .where(
                    MEMBER_TEAMS.MEMBER_ID.`in`(memberIds),
                    MEMBER_TEAMS.TEAM_ID.`in`(
                        dsl.select(TEAMS.TEAM_ID).from(TEAMS).where(TEAMS.COHORT_ID.eq(cohortId)),
                    ),
                )
                // REPEATABLE READ에서도 멤버 잠금 대기 중 커밋된 최신 연결을 확인한다.
                .forUpdate()
                .fetchGroups(MEMBER_TEAMS.MEMBER_ID, MEMBER_TEAMS.TEAM_ID)
        val changed =
            memberIds.filter { id ->
                val assigned = existing[id].orEmpty()
                if (teamId == null) assigned.isNotEmpty() else assigned != listOf(teamId)
            }.toSet()
        if (changed.isEmpty()) return emptySet()

        dsl.deleteFrom(MEMBER_TEAMS)
            .where(
                MEMBER_TEAMS.MEMBER_ID.`in`(changed),
                MEMBER_TEAMS.TEAM_ID.`in`(dsl.select(TEAMS.TEAM_ID).from(TEAMS).where(TEAMS.COHORT_ID.eq(cohortId))),
            ).execute()
        if (teamId != null) {
            val insert = dsl.insertInto(MEMBER_TEAMS, MEMBER_TEAMS.MEMBER_ID, MEMBER_TEAMS.TEAM_ID)
            changed.forEach { insert.values(it, teamId) }
            insert.execute()
        }
        return changed
    }
}
