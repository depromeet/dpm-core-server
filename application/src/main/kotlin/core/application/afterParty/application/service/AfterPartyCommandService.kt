package core.application.afterParty.application.service

import core.application.afterParty.application.exception.AfterPartyNotFoundException
import core.application.afterParty.application.exception.InviteTagNameNotFoundException
import core.application.member.application.exception.MemberNotFoundException
import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.enums.AfterPartyInviteTagEnum
import core.domain.afterParty.port.inbound.AfterPartyCommandUseCase
import core.domain.afterParty.port.inbound.AfterPartyInviteTagQueryUseCase
import core.domain.afterParty.port.inbound.AfterPartyInviteeCommandUseCase
import core.domain.afterParty.port.outbound.AfterPartyInviteTagPersistencePort
import core.domain.afterParty.port.outbound.AfterPartyPersistencePort
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.vo.RoleType
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.MemberQueryUseCase
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
    val cohortPersistencePort: CohortPersistencePort,
    val roleQueryUseCase: RoleQueryUseCase,
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
//        TODO : 태그 활용해서 init 지정 필요. 한 트랜잭션으로 묶으면 '18기 디퍼'가 insert 되기 전에 태그 조회가 일어나서, 태그 조회 시점에 되려나..?
        val afterParties: List<AfterParty> = afterPartyPersistencePort.findAll()
        val member: Member = memberQueryUseCase.getMemberById(memberId)

        afterParties.forEach { afterParty ->
            val afterPartyInvitee: AfterPartyInvitee =
                AfterPartyInvitee.create(
                    afterPartyId = afterParty.id ?: throw AfterPartyNotFoundException(),
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
        }
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
            try {
                roleQueryUseCase.findIdByName(roleType.code)
            } catch (ex: IllegalArgumentException) {
                throw InviteTagNameNotFoundException(tagName)
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
}
