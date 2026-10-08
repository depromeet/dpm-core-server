package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.service.cohort.MemberCohortService
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.enums.MemberAdmissionEventType
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberAdmissionEventPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberAdmissionService(
    private val members: MemberPersistencePort,
    private val events: MemberAdmissionEventPersistencePort,
    private val cohorts: CohortQueryUseCase,
    private val memberCohorts: MemberCohortService,
) {
    fun reject(memberId: Long) {
        val target = lockTarget(memberId)
        val cohortId = cohorts.getActiveCohortId().value
        if (target.status != MemberStatus.PENDING ||
            (target.cohortIds.isNotEmpty() && cohortId !in target.cohortIds)
        ) {
            throw BusinessException(MemberExceptionCode.MEMBER_REJECTION_TARGET_NOT_ALLOWED)
        }
        events.record(memberId, MemberAdmissionEventType.REJECTED)
        members.updateManagementFields(listOf(memberId), false, null, MemberStatus.REJECTED, emptySet())
    }

    fun reapply(memberId: Long) {
        val target = lockTarget(memberId)
        if (target.status == MemberStatus.PENDING) return
        if (target.status != MemberStatus.REJECTED) {
            throw BusinessException(MemberExceptionCode.MEMBER_REAPPLICATION_NOT_ALLOWED)
        }
        val cohortId = cohorts.getActiveCohortId()
        memberCohorts.addMemberToCohort(MemberId(memberId), cohortId)
        events.record(memberId, MemberAdmissionEventType.REAPPLIED)
        members.updateManagementFields(listOf(memberId), false, null, MemberStatus.PENDING, emptySet())
    }

    private fun lockTarget(memberId: Long): MemberApprovalTarget {
        if (memberId <= 0) throw BusinessException(MemberExceptionCode.INVALID_MEMBER_ID)
        val target = members.lockApprovalTargets(listOf(memberId)).singleOrNull() ?: throw MemberNotFoundException()
        if (target.isDeleted) throw MemberDeletedException()
        return target
    }
}
