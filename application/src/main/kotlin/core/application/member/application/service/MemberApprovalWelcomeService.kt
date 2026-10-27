package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
class MemberApprovalWelcomeService(private val members: MemberPersistencePort) {
    fun confirm(memberId: Long) {
        val member = members.lockApprovalTargets(listOf(memberId)).singleOrNull() ?: throw MemberNotFoundException()
        if (member.isDeleted || member.status == MemberStatus.WITHDRAWN) throw MemberDeletedException()
        if (member.status !in setOf(MemberStatus.ACTIVE, MemberStatus.INACTIVE)) {
            throw BusinessException(MemberExceptionCode.MEMBER_APPROVAL_WELCOME_NOT_ALLOWED)
        }
        members.markApprovalWelcomeSeen(memberId)
    }
}
