package core.application.member.application.service

import core.application.member.application.service.cohort.MemberCohortService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.member.presentation.request.InitMemberDataRequest
import core.application.security.oauth.token.JwtTokenInjector
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REFRESH_TOKEN
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberCommandService(
    private val memberPersistencePort: MemberPersistencePort,
    private val memberQueryService: MemberQueryService,
    private val memberTeamService: MemberTeamService,
    private val memberCohortService: MemberCohortService,
    private val tokenInjector: JwtTokenInjector,
    private val refreshTokenInvalidator: RefreshTokenInvalidator,
    private val memberRoleService: MemberRoleService,
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
            memberTeamService.addMemberToTeam(it.memberId, request.teamId)
            memberCohortService.addMemberToCohort(it.memberId)
        }
    }

    /**
     * 멤버를 탈퇴 처리(Soft Delete)하고, 클라이언트의 Refresh Token을 무효화 및 삭제함.
     *
     * @throws MemberNotFoundException
     *
     * @author LeeHanEum
     * @since 2025.09.01
     */
    fun withdraw(
        memberId: MemberId,
        response: HttpServletResponse,
    ) {
        tokenInjector.invalidateCookie(REFRESH_TOKEN, response)
        refreshTokenInvalidator.destroyRefreshToken(memberId)

        memberQueryService.getMemberById(memberId)
            .apply { softDelete() }
            .also { memberPersistencePort.save(it) }

        memberTeamService.deleteMemberFromTeam(memberId)
        memberCohortService.deleteMemberFromCohort(memberId)
        memberRoleService.revokeAllRoles(memberId)
    }

    fun activate(member: Member) {
        member.activate()
        memberPersistencePort.save(member)
    }
}
