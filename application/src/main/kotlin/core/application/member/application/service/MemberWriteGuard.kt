package core.application.member.application.service

import core.application.member.application.exception.MemberNotFoundException
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** 회원 삭제와 공유 기록 생성을 같은 회원 행 잠금으로 직렬화한다. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
class MemberWriteGuard(private val members: MemberPersistencePort) {
    fun lockAvailable(memberIds: List<Long>) {
        val ids = memberIds.distinct().sorted()
        val targets = members.lockApprovalTargets(ids)
        if (targets.map { it.memberId } != ids || targets.any { it.isDeleted || it.status == MemberStatus.WITHDRAWN }) {
            throw MemberNotFoundException()
        }
    }
}
