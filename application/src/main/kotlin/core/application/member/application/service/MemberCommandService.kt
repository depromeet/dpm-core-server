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
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberStatus
import core.domain.member.event.MemberActivatedEvent
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.ApplicationEventPublisher
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
    private val cohortQueryUseCase: CohortQueryUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher,
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
            val updatedMember =
                memberPersistencePort.save(
                    memberQueryService.getMemberById(it.memberId).apply {
                        updatePart(it.memberPart)
                        updateStatus(it.status)
                    },
                )
            memberTeamService.addMemberToTeam(it.memberId, it.team)
            memberCohortService.addMemberToCohort(it.memberId)
            initializeMemberDataForActiveMember(updatedMember)
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

        memberQueryService
            .getMemberById(memberId)
            .apply { softDelete() }
            .also { memberPersistencePort.save(it) }

        memberTeamService.deleteMemberFromTeam(memberId)
        memberCohortService.deleteMemberFromCohort(memberId)
        memberRoleService.revokeAllRoles(memberId)
    }

    fun activate(member: Member) {
        val memberId = requireNotNull(member.id) { "Activated member must have id" }
        val latestCohortValue = cohortQueryUseCase.getLatestCohortValue()
        val deeperRoleName = "${latestCohortValue}기 ${RoleType.Deeper.aliases.first()}"

        // Whitelist approval must always grant DEEPER authority and keep exactly one active latest cohort DEEPER role.
        memberAuthorityService.ensureAuthorityAssigned(memberId, DEEPER_AUTHORITY_ID)
        memberRoleService.replaceWithSingleRoleByName(memberId, deeperRoleName)

        member.activate()
        val activatedMember = memberPersistencePort.save(member)
        initializeMemberDataForActiveMember(activatedMember)
    }

    fun convertDeeperToOrganizer(request: ConvertDeeperToOrganizerRequest) {
        val memberId = request.memberId
        memberQueryService.getMemberById(memberId)
        val activeAuthorityIds = memberAuthorityService.getActiveAuthorityIdsByMemberId(memberId)
        if (activeAuthorityIds.none { it == DEEPER_AUTHORITY_ID }) {
            return
        }

        memberAuthorityService.ensureAuthorityAssigned(memberId, ORGANIZER_AUTHORITY_ID)
        memberAuthorityService.revokeAuthority(memberId, DEEPER_AUTHORITY_ID)
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
            val updatedMember =
                memberPersistencePort.save(
                    existMember.apply {
                        updateStatus(request.memberStatus)
                    },
                )
            initializeMemberDataForActiveMember(updatedMember)
        } else {
            throw MemberStatusAlreadyUpdatedException()
        }
    }

    fun initializeForNewCohortMember(
        memberId: MemberId,
        cohortId: CohortId,
    ) {
        memberCohortService.addMemberToCohort(memberId, cohortId)
        publishMemberActivatedEvent(memberId, cohortId)
    }

    private fun initializeMemberDataForActiveMember(member: Member) {
        if (member.status != MemberStatus.ACTIVE) {
            return
        }

        val memberId = requireNotNull(member.id) { "Active member must have id" }
        memberTeamService.ensureMemberTeamInitialized(memberId)
        val latestCohortId = cohortQueryUseCase.getLatestCohortId()
        memberCohortService.addMemberToCohort(memberId, latestCohortId)
        publishMemberActivatedEvent(memberId, latestCohortId)
    }

    private fun publishMemberActivatedEvent(
        memberId: MemberId,
        cohortId: CohortId,
    ) {
        applicationEventPublisher.publishEvent(
            MemberActivatedEvent.of(
                memberId = memberId,
                cohortId = cohortId,
            ),
        )
    }

    companion object {
        private const val DEEPER_AUTHORITY_ID = 1L
        private const val ORGANIZER_AUTHORITY_ID = 2L
    }
}
