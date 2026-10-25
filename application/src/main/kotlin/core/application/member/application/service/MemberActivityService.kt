package core.application.member.application.service

import core.application.cohort.application.exception.CohortNotFoundException
import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberExceptionCode
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.MemberActivityStatus
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberActivityPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
class MemberActivityService(
    private val activities: MemberActivityPersistencePort,
    private val members: MemberPersistencePort,
    private val cohorts: CohortQueryUseCase,
) {
    /** 모든 활동 변경 경로는 기수 → 회원 ID 순서로 잠근다. */
    fun lockCurrentCohort(): CohortId {
        activities.lockCohorts()
        return cohorts.findActiveCohortId() ?: throw CohortNotFoundException()
    }

    fun lockCohortChanges() {
        activities.lockCohorts()
    }

    fun requireRestorable(
        memberId: Long,
        cohortId: Long,
    ) {
        val activity = activities.find(memberId, cohortId)
        if (activity != null && (activity.status != MemberActivityStatus.ACTIVE || activity.suspendedAt != null)) {
            throw BusinessException(MemberExceptionCode.MEMBER_ACTIVITY_RESTORE_NOT_ALLOWED)
        }
    }

    fun activate(
        memberId: Long,
        cohortId: Long,
    ) {
        requireRestorable(memberId, cohortId)
        activities.transition(memberId, cohortId, MemberActivityStatus.ACTIVE, actorId())
    }

    fun change(
        memberIds: List<Long>,
        cohortId: Long,
        status: MemberActivityStatus,
    ) {
        memberIds.forEach { memberId ->
            val current =
                activities.find(memberId, cohortId)
                    ?: throw BusinessException(MemberExceptionCode.MEMBER_ACTIVITY_NOT_INITIALIZED)
            if (status == MemberActivityStatus.ACTIVE) {
                requireRestorable(memberId, cohortId)
            } else if (current.status == MemberActivityStatus.ENDED && status != MemberActivityStatus.ENDED) {
                throw BusinessException(MemberExceptionCode.MEMBER_ACTIVITY_RESTORE_NOT_ALLOWED)
            }
            activities.transition(memberId, cohortId, status, actorId())
        }
        members.updateManagementFields(
            memberIds,
            false,
            null,
            if (status == MemberActivityStatus.ACTIVE) MemberStatus.ACTIVE else MemberStatus.INACTIVE,
            emptySet(),
        )
    }

    /** 이미 잠근 기수 아래에서 회원을 순서대로 잠그고 종료한다. 정지 표시는 지우지 않는다. */
    fun endBeforeCohortSwitch(nextCohortId: Long) {
        activities.lockCohorts()
        val currentCohortId = cohorts.findActiveCohortId()?.value ?: return
        if (currentCohortId == nextCohortId) return
        val current = activities.findByCohort(currentCohortId)
        val ids = current.map { it.memberId }.distinct().sorted()
        if (ids.isEmpty()) return
        val targets = members.lockApprovalTargets(ids).filter { !it.isDeleted && it.status != MemberStatus.WITHDRAWN }
        val remainingIds = targets.map { it.memberId }
        remainingIds.forEach { activities.transition(it, currentCohortId, MemberActivityStatus.ENDED, actorId()) }
        // 가입 대기/반려로 전환된 계정을 승인 상태로 되돌리지 않는다.
        val approvedIds =
            targets.filter {
                it.status in setOf(MemberStatus.ACTIVE, MemberStatus.INACTIVE)
            }.map { it.memberId }
        members.updateManagementFields(approvedIds, false, null, MemberStatus.INACTIVE, emptySet())
    }

    // HTTP 요청에서는 Spring Security가 확인한 사용자만 기록한다. 배치/마이그레이션은 null이다.
    private fun actorId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (!authentication.isAuthenticated) throw IllegalStateException("Activity actor must be authenticated")
        return authentication.name.toLongOrNull()
            ?: throw IllegalStateException("Activity actor must be a member ID")
    }
}
