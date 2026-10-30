package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.InvalidMemberApprovalException
import core.application.member.application.exception.MemberApprovalTargetNotAllowedException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.vo.RoleType
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.aggregate.MemberCohort
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberCohortPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.port.outbound.MemberTeamPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
class MemberApprovalService(
    private val members: MemberPersistencePort,
    private val memberCohorts: MemberCohortPersistencePort,
    private val teams: MemberTeamPersistencePort,
    private val roles: MemberRolePersistencePort,
    private val cohorts: CohortQueryUseCase,
    private val roleQueries: RoleQueryUseCase,
    private val initializer: MemberActivationInitializer,
    private val activities: MemberActivityService,
) {
    @TrackMemberBadges
    fun approve(memberIds: List<Long?>) {
        if (memberIds.isEmpty() || memberIds.any { it == null || it <= 0 } ||
            memberIds.distinct().size != memberIds.size
        ) {
            throw InvalidMemberApprovalException()
        }
        val ids = memberIds.filterNotNull().sorted()
        val cohortId = activities.lockCurrentCohort()
        val targets = members.lockApprovalTargets(ids)
        if (targets.map { it.memberId } != ids) throw MemberNotFoundException()
        if (targets.any { it.isDeleted }) throw MemberDeletedException()
        val hasInvalidTarget =
            targets.any { target ->
                when (target.status) {
                    MemberStatus.PENDING -> target.cohortIds.isNotEmpty() && cohortId.value !in target.cohortIds
                    MemberStatus.ACTIVE, MemberStatus.INACTIVE -> target.cohortIds.isEmpty()
                    else -> true
                }
            }
        if (hasInvalidTarget) {
            throw MemberApprovalTargetNotAllowedException()
        }

        if (targets.any { it.status == MemberStatus.PENDING && it.profileCompletedAt == null }) {
            throw BusinessException(MemberExceptionCode.MEMBER_PROFILE_COMPLETION_REQUIRED)
        }
        // 현재 기수의 정지/종료는 승인 API로 복구할 수 없다. 새 기수 승인은 별도 기록으로 남긴다.
        targets.forEach { activities.requireRestorable(it.memberId, cohortId.value) }
        val pendingIds =
            targets.filter {
                it.status == MemberStatus.PENDING || cohortId.value !in it.cohortIds
            }.map { it.memberId }
        if (pendingIds.isEmpty()) return
        val roleId = roleQueries.findIdByName(RoleType.Deeper.code)
        roles.replaceCurrentCohortRoles(pendingIds, cohortId.value, roleId)
        teams.replaceCurrentCohortTeams(pendingIds, cohortId.value, null)
        pendingIds.forEach {
            memberCohorts.save(MemberCohort.of(MemberId(it), cohortId))
            activities.activate(it, cohortId.value)
        }
        members.updateManagementFields(pendingIds, false, null, MemberStatus.ACTIVE, emptySet())
        members.resetApprovalWelcome(targets.filter { it.status == MemberStatus.PENDING }.map { it.memberId })

        // 신규 승인 경로는 초기화까지 같은 트랜잭션에 포함한다. 실패하면 승인도 취소된다.
        pendingIds.forEach { initializer.initialize(MemberId(it), cohortId) }
    }
}
