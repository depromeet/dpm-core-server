package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.AppleLoginMemberRequiredException
import core.application.member.application.exception.InvalidMemberPartException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberStatusAlreadyUpdatedException
import core.application.member.application.service.cohort.MemberCohortService
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.member.presentation.request.AppleMemberProfileUpdateRequest
import core.application.member.presentation.request.InitMemberDataRequest
import core.application.member.presentation.request.UpdateMemberStatusRequest
import core.application.member.presentation.response.AppleMemberProfileUpdateResponse
import core.application.security.oauth.token.JwtTokenInjector
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.enums.OAuthProvider
import core.domain.member.event.MemberActivatedEvent
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
class MemberCommandService(
    private val memberPersistencePort: MemberPersistencePort,
    private val memberQueryService: MemberQueryService,
    private val memberTeamService: MemberTeamService,
    private val memberCohortService: MemberCohortService,
    private val tokenInjector: JwtTokenInjector,
    private val refreshTokenInvalidator: RefreshTokenInvalidator,
    private val memberRoleService: MemberRoleService,
    private val memberOAuthService: MemberOAuthService,
    private val memberCredentialPersistencePort: MemberCredentialPersistencePort,
    private val cohortQueryUseCase: CohortQueryUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * 회원 가입 시 멤버별 팀/파트/상태 정보를 주입함. (DEV)
     *
     * @throws MemberNotFoundException
     * @throws AuthorityNotFoundException
     *
     * @author LeeHanEum
     * @since 2025.08.02
     */
    @TrackMemberBadges
    fun initMemberDataAndApprove(request: InitMemberDataRequest) {
        guardLegacyAdmissionChange(request.members.map { it.memberId.value }, request.members.map { it.status })
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
    @TrackMemberBadges
    fun withdraw(
        memberId: MemberId,
        response: HttpServletResponse,
    ) {
        logger.warn { "Invalidating refresh token during member withdrawal for memberId=${memberId.value}" }
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
        memberOAuthService.deleteAllByMemberId(memberId)
        memberCredentialPersistencePort.deleteByMemberId(memberId)
        anonymizeWithdrawnMemberIdentity(withdrawnMember)
    }

    @TrackMemberBadges
    fun hardDelete(memberId: MemberId) {
        memberQueryService.getMemberById(memberId)
        memberPersistencePort.hardDeleteById(memberId)
    }

    @TrackMemberBadges
    fun updateAppleMemberProfile(
        memberId: MemberId,
        request: AppleMemberProfileUpdateRequest,
    ): AppleMemberProfileUpdateResponse {
        val target =
            memberPersistencePort.lockApprovalTargets(listOf(memberId.value)).singleOrNull()
                ?: throw MemberNotFoundException()
        if (target.isDeleted || target.status == MemberStatus.WITHDRAWN) {
            throw core.application.member.application.exception.MemberDeletedException()
        }
        val member =
            memberQueryService.getMemberById(memberId)

        if (memberOAuthService.findMemberIdsByProvider(OAuthProvider.APPLE).none { it == memberId }) {
            throw AppleLoginMemberRequiredException()
        }

        member.updateName(request.name.trim())
        val normalizedPart = request.part.trim().uppercase()
        if (normalizedPart != "UNASSIGNED") {
            val memberPart =
                runCatching { MemberPart.valueOf(normalizedPart) }
                    .getOrElse { throw InvalidMemberPartException() }
            member.updatePart(memberPart)
        }

        val savedMember = memberPersistencePort.save(member)
        return AppleMemberProfileUpdateResponse(
            memberId = requireNotNull(savedMember.id).value,
            name = savedMember.name,
            part = savedMember.part?.name,
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
    @TrackMemberBadges
    fun updateMemberStatus(request: UpdateMemberStatusRequest) {
        guardLegacyAdmissionChange(listOf(request.memberId.value), listOf(request.memberStatus))
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

    private fun guardLegacyAdmissionChange(
        memberIds: List<Long>,
        statuses: List<MemberStatus>,
    ) {
        val targets = memberPersistencePort.lockApprovalTargets(memberIds.distinct().sorted())
        if (MemberStatus.REJECTED in statuses || targets.any { it.status == MemberStatus.REJECTED }) {
            throw BusinessException(MemberExceptionCode.MEMBER_ADMISSION_CHANGE_NOT_ALLOWED)
        }
    }

    @TrackMemberBadges
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
        val latestCohortId = cohortQueryUseCase.getActiveCohortId()
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
