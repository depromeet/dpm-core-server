package core.application.afterParty.application.service

import core.application.afterParty.application.exception.AfterPartyNotFoundException
import core.application.afterParty.application.exception.InviteTagNameNotFoundException
import core.application.member.application.service.authority.MemberAuthorityService
import core.application.afterParty.presentation.response.AfterPartyInviteeCompensationResponse
import core.application.member.application.exception.MemberNotFoundException
import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.enums.AfterPartyInviteTagEnum
import core.domain.afterParty.port.inbound.AfterPartyCommandUseCase
import core.domain.afterParty.port.inbound.AfterPartyInviteTagQueryUseCase
import core.domain.afterParty.port.inbound.AfterPartyInviteeCommandUseCase
import core.domain.afterParty.port.outbound.AfterPartyInviteePersistencePort
import core.domain.afterParty.port.outbound.AfterPartyInviteTagPersistencePort
import core.domain.afterParty.port.outbound.AfterPartyPersistencePort
import core.domain.authorization.vo.RoleType
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberCohort
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.enums.MemberStatus
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class AfterPartyCommandService(
    val afterPartyPersistencePort: AfterPartyPersistencePort,
    val afterPartyInviteeCommandUseCase: AfterPartyInviteeCommandUseCase,
    val afterPartyInviteTagPersistencePort: AfterPartyInviteTagPersistencePort,
    val memberQueryUseCase: MemberQueryUseCase,
    val afterPartyInviteTagQueryUseCase: AfterPartyInviteTagQueryUseCase,
    val afterPartyInviteePersistencePort: AfterPartyInviteePersistencePort,
    val cohortPersistencePort: CohortPersistencePort,
    val memberAuthorityService: MemberAuthorityService,
) : AfterPartyCommandUseCase {
    override fun createAfterParty(
        afterParty: AfterParty,
        afterPartyInviteTags: List<AfterPartyInviteTagEnum>,
        authorMemberId: MemberId,
    ) {
        val inviteTagSpecs =
            afterPartyInviteTags.map { tag ->
                InviteTagSpec(
                    cohortId = tag.cohortId,
                    authorityId = tag.authorityId,
                    tagName = tag.tagName,
                )
            }
        createAfterPartyInternal(afterParty, inviteTagSpecs, authorMemberId)
    }

    override fun createAfterPartyByInviteTagNames(
        afterParty: AfterParty,
        inviteTagNames: List<String>,
        authorMemberId: MemberId,
    ) {
        val inviteTagSpecs = resolveInviteTagSpecs(inviteTagNames)
        createAfterPartyInternal(afterParty, inviteTagSpecs, authorMemberId)
    }

    override fun updateAfterParty(afterParty: AfterParty) {
        updateAfterPartyInternal(afterParty)
    }

    override fun initializeForNewCohortMember(
        memberId: MemberId,
        cohortId: CohortId,
    ) {
        inviteOpenAfterPartiesForMember(memberId, cohortId)
    }

    override fun compensateMissingInviteesForOpenAfterParties(): Int {
        val members = memberQueryUseCase.getAll()

        return members
            .asSequence()
            .filter { it.status == MemberStatus.ACTIVE || it.status == MemberStatus.INACTIVE }
            .flatMap { member ->
                val memberId = member.id ?: return@flatMap emptySequence()
                member.memberCohorts
                    .asSequence()
                    .map(MemberCohort::cohortId)
                    .distinct()
                    .map { cohortId -> memberId to cohortId }
            }.sumOf { (memberId, cohortId) ->
                inviteOpenAfterPartiesForMember(memberId, cohortId)
            }
    }

    private fun inviteOpenAfterPartiesForMember(
        memberId: MemberId,
        cohortId: CohortId,
    ): Int {
        val now = Instant.now()
        val memberAuthorityIds = memberAuthorityService.getActiveAuthorityIdsByMemberId(memberId).toSet()

        if (memberAuthorityIds.isEmpty()) {
            return 0
        }

        val afterParties: List<AfterParty> =
            afterPartyPersistencePort
                .findAll()
                .filter { !now.isAfter(it.closedAt) }
        val member: Member = memberQueryUseCase.getMemberById(memberId)
        var createdInviteeCount = 0

        afterParties.forEach { afterParty ->
            val afterPartyId = afterParty.id ?: throw AfterPartyNotFoundException()
            val inviteTags = afterPartyInviteTagPersistencePort.findByAfterPartyId(afterPartyId)
            val shouldInvite =
                inviteTags.any { inviteTag ->
                    inviteTag.cohortId == cohortId && inviteTag.authorityId in memberAuthorityIds
                }

            if (!shouldInvite) {
                return@forEach
            }
            if (afterPartyInviteePersistencePort.findByMemberIdAndAfterPartyId(memberId, afterPartyId) != null) {
                return@forEach
            }

            val afterPartyInvitee: AfterPartyInvitee =
                AfterPartyInvitee.create(
                    afterPartyId = afterPartyId,
                    memberId = memberId,
                    invitedAt = Instant.now(),
                )
            val authorMember: Member = memberQueryUseCase.getMemberById(afterParty.authorMemberId)

            afterPartyInviteeCommandUseCase.createAfterPartyInvitee(
                afterPartyInvitee = afterPartyInvitee,
                afterParty = afterParty,
                authorMember = authorMember,
                inviteeMember = member,
            )
            createdInviteeCount += 1
        }

        return createdInviteeCount
    }

    private fun createAfterPartyInternal(
        afterParty: AfterParty,
        inviteTagSpecs: List<InviteTagSpec>,
        authorMemberId: MemberId,
    ) {
        val authorMember: Member = memberQueryUseCase.getMemberById(authorMemberId)

        val createdAfterParty: AfterParty =
            afterPartyPersistencePort.save(
                afterParty = afterParty,
                authorMember = authorMember,
            )

        val normalizedInviteTagSpecs = inviteTagSpecs.distinctBy { it.tagName }
        if (normalizedInviteTagSpecs.isEmpty()) {
            return
        }

        normalizedInviteTagSpecs.forEach { tag ->
            afterPartyInviteTagPersistencePort.save(
                afterPartyInviteTag =
                    AfterPartyInviteTag.create(
                        afterPartyId = createdAfterParty.id ?: throw AfterPartyNotFoundException(),
                        cohortId = tag.cohortId,
                        authorityId = tag.authorityId,
                        tagName = tag.tagName,
                    ),
                afterParty = createdAfterParty,
            )
        }

        val inviteeMemberIds: List<MemberId> =
            normalizedInviteTagSpecs
                .flatMap { tag ->
                    memberQueryUseCase.findAllMemberIdsByCohortIdAndAuthorityId(
                        tag.cohortId,
                        tag.authorityId,
                    )
                }.distinct()

        if (inviteeMemberIds.isEmpty()) {
            return
        }

        val inviteeMembers: List<Member> = memberQueryUseCase.getMembersByIds(inviteeMemberIds)

        val inviteeList: List<AfterPartyInvitee> =
            inviteeMembers.map { member ->
                AfterPartyInvitee.create(
                    afterPartyId = createdAfterParty.id ?: throw AfterPartyNotFoundException(),
                    memberId = member.id ?: throw MemberNotFoundException(),
                    invitedAt = createdAfterParty.createdAt ?: throw AfterPartyNotFoundException(),
                )
            }
        inviteeList.forEach { invitee ->
            val inviteeMember: Member =
                inviteeMembers.find { it.id == invitee.memberId }
                    ?: throw MemberNotFoundException()
            afterPartyInviteeCommandUseCase.createAfterPartyInvitee(
                afterPartyInvitee = invitee,
                afterParty = createdAfterParty,
                authorMember = authorMember,
                inviteeMember = inviteeMember,
            )
        }
    }

    private fun updateAfterPartyInternal(afterParty: AfterParty) {
        val existingAfterParty: AfterParty =
            afterPartyPersistencePort.findById(afterParty.id ?: throw AfterPartyNotFoundException())
                ?: throw AfterPartyNotFoundException()

//        TODO : 최식 초대 범위 수정 시 기존 멤버 제거하는 로직 구현 필요
        val updatedAfterParty: AfterParty =
            existingAfterParty.update(
                title = afterParty.title,
                description = afterParty.description,
                scheduledAt = afterParty.scheduledAt,
                closedAt = afterParty.closedAt,
                canEditAfterApproval = afterParty.canEditAfterApproval,
            )

        afterPartyPersistencePort.update(
            afterParty = updatedAfterParty,
        )
    }

    private fun resolveInviteTagSpecs(inviteTagNames: List<String>): List<InviteTagSpec> =
        inviteTagNames.map { tagName ->
            resolveInviteTagSpec(tagName)
        }

    private fun resolveInviteTagSpec(tagName: String): InviteTagSpec {
        val enumTag = AfterPartyInviteTagEnum.entries.firstOrNull { it.tagName == tagName }
        if (enumTag != null) {
            return InviteTagSpec(
                cohortId = enumTag.cohortId,
                authorityId = enumTag.authorityId,
                tagName = enumTag.tagName,
            )
        }

        val storedTag =
            afterPartyInviteTagQueryUseCase.findDistinctByTagName(tagName).firstOrNull()
        if (storedTag != null) {
            return InviteTagSpec(
                cohortId = storedTag.cohortId,
                authorityId = storedTag.authorityId,
                tagName = storedTag.tagName,
            )
        }

        val cohortId = extractCohortId(tagName) ?: throw InviteTagNameNotFoundException(tagName)
        val roleType = RoleType.from(tagName)
        if (roleType == RoleType.Guest) {
            throw InviteTagNameNotFoundException(tagName)
        }

        val authorityId =
            when (roleType) {
                RoleType.Deeper -> DEEPER_AUTHORITY_ID
                RoleType.Organizer -> ORGANIZER_AUTHORITY_ID
                RoleType.Core, RoleType.Guest -> throw InviteTagNameNotFoundException(tagName)
            }

        return InviteTagSpec(
            cohortId = cohortId,
            authorityId = authorityId,
            tagName = tagName,
        )
    }

    private fun extractCohortId(tagName: String): CohortId? {
        val cohortValue =
            Regex("\\d+")
                .find(tagName)
                ?.value
                ?: return null

        val cohort =
            try {
                cohortPersistencePort.findByValue(cohortValue)
            } catch (ex: Exception) {
                null
            } ?: return null
        return cohort.id
    }

    private data class InviteTagSpec(
        val cohortId: CohortId,
        val authorityId: Long,
        val tagName: String,
    )

    companion object {
        private const val DEEPER_AUTHORITY_ID = 1L
        private const val ORGANIZER_AUTHORITY_ID = 2L
    }
}
