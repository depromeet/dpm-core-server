package com.server.dpmcore.member.member.application

import com.server.dpmcore.authority.domain.model.AuthorityType.DEEPER
import com.server.dpmcore.member.member.application.exception.MemberNotFoundException
import com.server.dpmcore.member.member.domain.port.outbound.MemberPersistencePort
import com.server.dpmcore.member.member.presentation.request.InitMemberDataRequest
import com.server.dpmcore.member.memberAuthority.application.MemberAuthorityService
import com.server.dpmcore.member.memberCohort.application.MemberCohortService
import com.server.dpmcore.member.memberTeam.application.MemberTeamService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberCommandService(
    private val memberPersistencePort: MemberPersistencePort,
    private val memberQueryService: MemberQueryService,
    private val memberAuthorityService: MemberAuthorityService,
    private val memberTeamService: MemberTeamService,
    private val memberCohortService: MemberCohortService,
) {
    /**
     * 회원 가입 시 팀 정보 및 파트 정보를 주입하고, 멤버를 ACTIVE 상태로 변경함. (DEV)
     *
     * @throws MemberNotFoundException
     * @throws AuthorityNotFoundException
     *
     * @author LeeHanEum
     * @since 2025.08.02
     */
    fun initMemberDataAndApprove(request: InitMemberDataRequest) {
        request.members.forEach {
            memberPersistencePort.save(
                memberQueryService.getMemberById(it.memberId).apply {
                    updatePart(it.memberPart)
                    activate()
                },
            )
            memberAuthorityService.setMemberAuthorityByMemberId(it.memberId, DEEPER)
            memberTeamService.addMemberToTeam(it.memberId, request.teamId)
            memberCohortService.addMemberToCohort(it.memberId)
        }
    }
}
