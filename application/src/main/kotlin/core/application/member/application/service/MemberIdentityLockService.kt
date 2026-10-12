package core.application.member.application.service

import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberOAuthConflictException
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberMergePersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.MANDATORY)
class MemberIdentityLockService(
    private val members: MemberPersistencePort,
    private val identities: MemberMergePersistencePort,
) {
    fun lockMember(memberId: MemberId) {
        val target =
            members.lockApprovalTargets(listOf(memberId.value)).singleOrNull() ?: throw MemberNotFoundException()
        if (target.isDeleted || target.status == MemberStatus.WITHDRAWN) throw MemberDeletedException()
    }

    /** Reject stale login observations; a fresh login resolves the retained owner. */
    fun lockOAuth(observed: MemberOAuth) {
        // Missing rows are legacy orphan references; retained deleted rows must never be recovered.
        val owner = members.lockApprovalTargets(listOf(observed.memberId.value)).singleOrNull()
        if (owner != null && (owner.isDeleted || owner.status == MemberStatus.WITHDRAWN)) {
            throw MemberDeletedException()
        }
        val current = identities.lockOAuths(listOf(observed.memberId.value)).find { it.id == observed.id }
        if (current == null || current.memberId != observed.memberId ||
            current.externalId != observed.externalId || current.provider != observed.provider
        ) {
            throw MemberOAuthConflictException()
        }
    }
}
