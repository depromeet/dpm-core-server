package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.common.exception.GlobalExceptionCode
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberSharedRecordsException
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
    private val eventPublisher: ApplicationEventPublisher,
) {
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
        // 승인 및 관리 수정과 같은 회원 행을 같은 순서로 잠근다.
        val targets = members.lockApprovalTargets(ids)
        if (targets.map { it.memberId } != ids || targets.any { it.isDeleted || it.status == MemberStatus.WITHDRAWN }) {
            throw MemberNotFoundException()
        }
        val approvedIds =
            targets.filter {
                it.status == MemberStatus.ACTIVE || it.status == MemberStatus.INACTIVE
            }.map { it.memberId }
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
