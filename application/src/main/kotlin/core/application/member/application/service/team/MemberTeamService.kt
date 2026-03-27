package core.application.member.application.service.team

import core.domain.member.aggregate.MemberTeam
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberTeamPersistencePort
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MemberTeamService(
    private val memberTeamPersistencePort: MemberTeamPersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    @Value("\${member.default-team-id:0}")
    private val defaultTeamId: Int,
) {
    /**
     * 팀에 멤버를 추가함.
     *
     * @author LeeHanEum
     * @since 2025.08.02
     */
    fun addMemberToTeam(
        memberId: MemberId,
        teamId: TeamId,
    ) {
        memberTeamPersistencePort.save(MemberTeam.of(memberId, teamId))
    }

    fun ensureMemberTeamInitialized(memberId: MemberId) {
        if (defaultTeamId <= 0) {
            return
        }

        if (memberPersistencePort.findMemberTeamNumberByMemberId(memberId) != null) {
            return
        }

        memberTeamPersistencePort.save(
            MemberTeam.of(
                memberId = memberId,
                teamId = TeamId(defaultTeamId.toLong()),
            ),
        )
    }

    /**
     * 멤버 탈퇴 시에, 멤버를 팀에서 제거(Hard Delete)하여 팀 정보 조회 시 노출되지 않도록 함.
     *
     * @author LeeHanEum
     * @since 2025.09.02
     */
    fun deleteMemberFromTeam(memberId: MemberId) {
        memberTeamPersistencePort.deleteByMemberId(memberId.value)
    }
}
