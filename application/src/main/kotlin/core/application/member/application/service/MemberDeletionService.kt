package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.common.exception.GlobalExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberDeletionService(
    private val members: MemberPersistencePort,
) {
    fun delete(memberIds: List<Long?>) {
        if (memberIds.isEmpty() || memberIds.any { it == null || it <= 0 } ||
            memberIds.distinct().size != memberIds.size
        ) {
            throw BusinessException(GlobalExceptionCode.INVALID_INPUT)
        }
        val ids = memberIds.filterNotNull().sorted()
        // 승인 및 관리 수정과 같은 회원 행을 같은 순서로 잠근다.
        val targets = members.lockApprovalTargets(ids)
        if (targets.map { it.memberId } != ids || targets.any { it.isDeleted || it.status == MemberStatus.WITHDRAWN }) {
            throw MemberNotFoundException()
        }
        members.softDeleteMembers(ids)
    }
}
