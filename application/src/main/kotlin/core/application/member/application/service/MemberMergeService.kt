package core.application.member.application.service

import core.application.member.application.exception.InvalidMemberMergeException
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberOAuthConflictException
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberMergePersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
class MemberMergeService(
    private val members: MemberPersistencePort,
    private val merge: MemberMergePersistencePort,
    private val cohorts: CohortQueryUseCase,
    private val approval: MemberApprovalService,
) {
    @TrackMemberBadges
    fun mergeAndApprove(
        retainedId: Long?,
        sourceId: Long?,
    ) {
        if (retainedId == null || sourceId == null || retainedId <= 0 || sourceId <= 0 || retainedId == sourceId) {
            throw InvalidMemberMergeException()
        }
        val ids = listOf(retainedId, sourceId).sorted()
        val cohortId = cohorts.getActiveCohortId().value
        val targets = members.lockApprovalTargets(ids)
        if (targets.map { it.memberId } != ids) throw MemberNotFoundException()
        if (targets.any {
                it.isDeleted || it.status != MemberStatus.PENDING ||
                    (it.cohortIds.isNotEmpty() && cohortId !in it.cohortIds)
            }
        ) {
            throw InvalidMemberMergeException()
        }
        if (merge.hasPasswordCredential(ids)) throw InvalidMemberMergeException()
        val oauths = merge.lockOAuths(ids)
        if (ids.any { id -> oauths.none { it.memberId.value == id } }) throw InvalidMemberMergeException()
        if (oauths.groupBy { it.provider }.values.any { group -> group.map { it.externalId }.distinct().size > 1 }) {
            throw MemberOAuthConflictException()
        }
        merge.transferOAuths(sourceId, retainedId)
        merge.softDeleteSource(sourceId)
        approval.approve(listOf(retainedId))
    }
}
