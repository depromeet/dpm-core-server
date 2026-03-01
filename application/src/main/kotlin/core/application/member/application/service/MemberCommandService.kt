package core.application.member.application.service

import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberStatusAlreadyUpdatedException
import core.application.member.application.service.authority.MemberAuthorityService
import core.application.member.application.service.cohort.MemberCohortService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.member.presentation.request.ConvertDeeperToOrganizerRequest
import core.application.member.presentation.request.InitMemberDataRequest
import core.application.member.presentation.request.UpdateMemberStatusRequest
import core.application.security.oauth.token.JwtTokenInjector
import core.domain.authorization.vo.RoleType
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
import jakarta.servlet.http.HttpServletResponse
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
    private val memberAuthorityService: MemberAuthorityService,
) {
    /**
     * 회원 가입 시 멤버별 팀/파트/상태 정보를 주입함. (DEV)
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
                    updateStatus(it.status)
                },
            )
            memberTeamService.addMemberToTeam(it.memberId, it.team)
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
        tokenInjector.invalidateRefreshToken(response)
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
        val memberId = member.id ?: return
        memberRoleService.ensureRoleAssigned(memberId, RoleType.Deeper)
        memberAuthorityService.ensureAuthorityAssigned(memberId, RoleType.Deeper.code)
    }

    fun convertDeeperToOrganizer(request: ConvertDeeperToOrganizerRequest) {
        val memberId = request.memberId
        memberQueryService.getMemberById(memberId)

        memberRoleService.ensureRoleAssigned(memberId, RoleType.Organizer)
        memberRoleService.revokeRole(memberId, RoleType.Deeper)

        memberAuthorityService.ensureAuthorityAssigned(memberId, RoleType.Organizer.code)
        memberAuthorityService.revokeAuthority(memberId, RoleType.Deeper.code)
    }

    /**
     * 멤버의 상태(status)를 변경함.
     * 개발 중 멤버 상태를 컨트롤하기 위해 사용합니다.(PENDING/ACTIVE)
     *
     * @throws MemberNotFoundException
     * @throws MemberStatusAlreadyUpdatedException
     *
     * @author junwon
     * @since 2026.01.09
     */
    fun updateMemberStatus(request: UpdateMemberStatusRequest) {
        val existMember = memberQueryService.getMemberById(request.memberId)

        if (existMember.status != request.memberStatus) {
            memberPersistencePort.save(
                existMember.apply {
                    updateStatus(request.memberStatus)
                },
            )
        } else {
            throw MemberStatusAlreadyUpdatedException()
        }
    }
}
