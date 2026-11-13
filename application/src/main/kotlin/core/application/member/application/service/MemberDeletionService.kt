package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.common.exception.GlobalExceptionCode
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberSharedRecordsException
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
class MemberDeletionService(
    private val members: MemberPersistencePort,
    private val activities: MemberActivityService,
    private val cohorts: CohortQueryUseCase,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * 운영진 삭제. 가입 대기자는 소프트 삭제하고, 승인 회원은 활성 기수 소속일 때만 영구 삭제한다.
     * 지난 기수에만 속한 승인 회원은 출석·역할 기록까지 사라지므로 거절하고 MASTER 영구 삭제로만 처리한다.
     */
    @TrackMemberBadges
    fun delete(
        memberIds: List<Long?>,
        actorMemberId: Long,
    ) {
        if (memberIds.isEmpty() || memberIds.any { it == null || it <= 0 } ||
            memberIds.distinct().size != memberIds.size
        ) {
            throw BusinessException(GlobalExceptionCode.INVALID_INPUT)
        }
        if (actorMemberId in memberIds) {
            throw BusinessException(MemberExceptionCode.MEMBER_SELF_DELETION_NOT_ALLOWED)
        }
        val ids = memberIds.filterNotNull().sorted()
        // 기수 전환·관리 수정과 같은 기수 → 회원 순서로 잠가, 확인하는 동안 활성 기수가 바뀌지 않게 한다.
        activities.lockCohortChanges()
        val activeCohortId = cohorts.findActiveCohortId()?.value
        val targets = members.lockApprovalTargets(ids)
        if (targets.map { it.memberId } != ids || targets.any { it.isDeleted || it.status == MemberStatus.WITHDRAWN }) {
            throw MemberNotFoundException()
        }
        val approvedTargets =
            targets.filter {
                it.status == MemberStatus.ACTIVE || it.status == MemberStatus.INACTIVE
            }
        if (approvedTargets.any { activeCohortId == null || activeCohortId !in it.cohortIds }) {
            throw BusinessException(MemberExceptionCode.MEMBER_PAST_COHORT_DELETION_NOT_ALLOWED)
        }
        val approvedIds = approvedTargets.map { it.memberId }
        val blockedMemberIds = members.findSharedRecordOwnerIds(approvedIds)
        if (blockedMemberIds.isNotEmpty()) {
            throw MemberSharedRecordsException(blockedMemberIds)
        }
        val pendingIds = ids - approvedIds.toSet()
        if (pendingIds.isNotEmpty()) members.softDeleteMembers(pendingIds)
        approvedIds.forEach { memberId ->
            val imageObjectKeys = members.hardDeleteById(MemberId(memberId))
            eventPublisher.publishEvent(MemberPurgedEvent(memberId, imageObjectKeys))
        }
    }
}
