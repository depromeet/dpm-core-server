package core.application.member.application.service.team

import core.domain.member.aggregate.MemberTeam
import core.domain.member.port.outbound.MemberTeamPersistencePort
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId
import org.springframework.stereotype.Service

@Service
class MemberTeamService(
    private val memberTeamPersistencePort: MemberTeamPersistencePort,
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
