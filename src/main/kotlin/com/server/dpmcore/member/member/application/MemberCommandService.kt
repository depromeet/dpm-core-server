package com.server.dpmcore.member.member.application

import com.server.dpmcore.authority.domain.model.AuthorityType.DEEPER
import com.server.dpmcore.member.member.application.exception.MemberNotFoundException
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.port.outbound.MemberPersistencePort
import com.server.dpmcore.member.member.presentation.request.InitMemberDataRequest
import com.server.dpmcore.member.memberAuthority.application.MemberAuthorityService
import com.server.dpmcore.member.memberCohort.application.MemberCohortService
import com.server.dpmcore.member.memberTeam.application.MemberTeamService
import com.server.dpmcore.refreshToken.domain.port.inbound.RefreshTokenInvalidator
import com.server.dpmcore.security.oauth.token.JwtTokenInjector
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REFRESH_TOKEN
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
    private val tokenInjector: JwtTokenInjector,
    private val refreshTokenInvalidator: RefreshTokenInvalidator,
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
        memberAuthorityService.revokeAllAuthorities(memberId)
    }
}
