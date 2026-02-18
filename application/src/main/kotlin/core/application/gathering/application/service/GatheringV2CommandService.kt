package core.application.gathering.application.service

import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.gathering.application.exception.InviteTagNameNotFoundException
import core.application.member.application.exception.MemberNotFoundException
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.vo.RoleType
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2InviteTag as GatheringV2InviteTagAggregate
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.enums.GatheringV2InviteTag
import core.domain.gathering.port.inbound.GatheringV2CommandUseCase
import core.domain.gathering.port.inbound.GatheringV2InviteTagQueryUseCase
import core.domain.gathering.port.inbound.GatheringV2InviteeCommandUseCase
import core.domain.gathering.port.outbound.GatheringV2InviteTagPersistencePort
import core.domain.gathering.port.outbound.GatheringV2PersistencePort
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GatheringV2CommandService(
    val gatheringV2PersistencePort: GatheringV2PersistencePort,
    val gatheringV2InviteeCommandUseCase: GatheringV2InviteeCommandUseCase,
    val gatheringV2InviteTagPersistencePort: GatheringV2InviteTagPersistencePort,
    val memberQueryUseCase: MemberQueryUseCase,
    val gatheringV2InviteTagQueryUseCase: GatheringV2InviteTagQueryUseCase,
    val cohortPersistencePort: CohortPersistencePort,
    val roleQueryUseCase: RoleQueryUseCase,
) : GatheringV2CommandUseCase {
    @Transactional
    override fun createGatheringV2(
        gatheringV2: GatheringV2,
        gatheringV2InviteTags: List<GatheringV2InviteTag>,
        authorMemberId: MemberId,
    ) {
        val inviteTagSpecs =
            gatheringV2InviteTags.map { tag ->
                InviteTagSpec(
                    cohortId = tag.cohortId,
                    authorityId = tag.authorityId,
                    tagName = tag.tagName,
                )
            }
        createGatheringV2Internal(gatheringV2, inviteTagSpecs, authorMemberId)
    }

    @Transactional
    override fun createGatheringV2ByInviteTagNames(
        gatheringV2: GatheringV2,
        inviteTagNames: List<String>,
        authorMemberId: MemberId,
    ) {
        val inviteTagSpecs = resolveInviteTagSpecs(inviteTagNames)
        createGatheringV2Internal(gatheringV2, inviteTagSpecs, authorMemberId)
    }

    private fun createGatheringV2Internal(
        gatheringV2: GatheringV2,
        inviteTagSpecs: List<InviteTagSpec>,
        authorMemberId: MemberId,
    ) {
        val authorMember: Member = memberQueryUseCase.getMemberById(authorMemberId)

        val createdGatheringV2: GatheringV2 =
            gatheringV2PersistencePort.save(
                gatheringV2 = gatheringV2,
                authorMember = authorMember,
            )

        val normalizedInviteTagSpecs = inviteTagSpecs.distinctBy { it.tagName }
        if (normalizedInviteTagSpecs.isEmpty()) {
            return
        }

        normalizedInviteTagSpecs.forEach { tag ->
            gatheringV2InviteTagPersistencePort.save(
                gatheringV2InviteTag = GatheringV2InviteTagAggregate.create(
                    gatheringId = createdGatheringV2.id ?: throw GatheringNotFoundException(),
                    cohortId = tag.cohortId,
                    authorityId = tag.authorityId,
                    tagName = tag.tagName,
                ),
                gatheringV2 = createdGatheringV2,
            )
        }

        val inviteeMemberIds: List<MemberId> = normalizedInviteTagSpecs.flatMap { tag ->
            memberQueryUseCase.findAllMemberIdsByCohortIdAndAuthorityId(
                tag.cohortId,
                tag.authorityId,
            )
        }.distinct()

        if (inviteeMemberIds.isEmpty()) {
            return
        }

        val inviteeMembers: List<Member> = memberQueryUseCase.getMembersByIds(inviteeMemberIds)

        val inviteeList: List<GatheringV2Invitee> =
            inviteeMembers.map { member ->
                GatheringV2Invitee.create(
                    gatheringV2Id = createdGatheringV2.id ?: throw GatheringNotFoundException(),
                    memberId = member.id ?: throw MemberNotFoundException(),
                    invitedAt = createdGatheringV2.createdAt ?: throw GatheringNotFoundException(),
                )
            }
        inviteeList.forEach { invitee ->
            val inviteeMember: Member =
                inviteeMembers.find { it.id == invitee.memberId }
                    ?: throw MemberNotFoundException()
            gatheringV2InviteeCommandUseCase.createGatheringV2Invitee(
                gatheringV2Invitee = invitee,
                gatheringV2 = createdGatheringV2,
                authorMember = authorMember,
                inviteeMember = inviteeMember,
            )
        }
    }

    private fun resolveInviteTagSpecs(inviteTagNames: List<String>): List<InviteTagSpec> =
        inviteTagNames.map { tagName ->
            resolveInviteTagSpec(tagName)
        }

    private fun resolveInviteTagSpec(tagName: String): InviteTagSpec {
        val enumTag = GatheringV2InviteTag.entries.firstOrNull { it.tagName == tagName }
        if (enumTag != null) {
            return InviteTagSpec(
                cohortId = enumTag.cohortId,
                authorityId = enumTag.authorityId,
                tagName = enumTag.tagName,
            )
        }

        val storedTag =
            gatheringV2InviteTagQueryUseCase.findDistinctByTagName(tagName).firstOrNull()
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
