package core.application.member.application.service

import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberStatusAlreadyUpdatedException
import core.application.member.application.service.authority.MemberAuthorityService
import core.application.member.application.service.cohort.MemberCohortService
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.member.presentation.request.ConvertDeeperToOrganizerRequest
import core.application.member.presentation.request.InitMemberDataRequest
import core.application.member.presentation.request.UpdateMemberStatusRequest
import core.application.security.oauth.token.JwtTokenInjector
import core.domain.authorization.vo.RoleType
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.constant.AuthorityConstants.DEEPER_AUTHORITY_ID
import core.domain.member.constant.AuthorityConstants.ORGANIZER_AUTHORITY_ID
import core.domain.member.enums.MemberStatus
import core.domain.member.event.MemberActivatedEvent
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

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
    private val memberOAuthService: MemberOAuthService,
    private val memberCredentialPersistencePort: MemberCredentialPersistencePort,
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
            memberTeamService.addMemberToTeam(it.memberId, it.teamId)
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

        val withdrawnMember =
            memberQueryService
                .getMemberById(memberId)
                .apply { softDelete() }
                .also { memberPersistencePort.save(it) }

        memberTeamService.deleteMemberFromTeam(memberId)
        memberCohortService.deleteMemberFromCohort(memberId)
        memberRoleService.revokeAllRoles(memberId)
        memberAuthorityService.revokeAllAuthorities(memberId)
        memberOAuthService.deleteAllByMemberId(memberId)
        memberCredentialPersistencePort.deleteByMemberId(memberId)
        anonymizeWithdrawnMemberIdentity(withdrawnMember)
    }

    fun hardDelete(memberId: MemberId) {
        memberQueryService.getMemberById(memberId)
        memberPersistencePort.hardDeleteById(memberId)
    }

    fun activate(member: Member) {
        val memberId = requireNotNull(member.id) { "Activated member must have id" }
        val latestCohortValue = cohortQueryUseCase.getLatestCohortValue()
        val deeperRoleName = "${latestCohortValue}기 ${RoleType.Deeper.aliases.first()}"

        // Whitelist approval must always grant DEEPER authority and keep exactly one active latest cohort DEEPER role.
        memberAuthorityService.ensureAuthorityAssigned(memberId, AuthorityId(DEEPER_AUTHORITY_ID))
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

        memberAuthorityService.ensureAuthorityAssigned(memberId, AuthorityId(ORGANIZER_AUTHORITY_ID))
        memberAuthorityService.revokeAuthority(memberId, AuthorityId(DEEPER_AUTHORITY_ID))
        memberRoleService.replaceWithSingleRoleByName(
            memberId = memberId,
            roleName = "${cohortQueryUseCase.getLatestCohortValue()}기 ${RoleType.Organizer.aliases.first()}",
        )
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

    private fun anonymizeWithdrawnMemberIdentity(member: Member) {
        val memberId = requireNotNull(member.id) { "Withdrawn member must have id" }
        val uniqueSuffix = "${memberId.value}-${Instant.now().toEpochMilli()}"
        val anonymizedEmail = "withdrawn+$uniqueSuffix@withdrawn.local"

        memberPersistencePort.anonymizeIdentity(
            memberId = memberId,
            email = anonymizedEmail,
            signupEmail = anonymizedEmail,
        )
    }
}
