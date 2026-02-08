package core.application.gathering.application.service

import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.member.application.exception.MemberNotFoundException
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2InviteTag as GatheringV2InviteTagAggregate
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.enums.GatheringV2InviteTag
import core.domain.gathering.port.inbound.GatheringV2CommandUseCase
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
) : GatheringV2CommandUseCase {
    @Transactional
    override fun createGatheringV2(
        gatheringV2: GatheringV2,
        gatheringV2InviteTags: List<GatheringV2InviteTag>,
        authorMemberId: MemberId,
    ) {
        val authorMember: Member = memberQueryUseCase.getMemberById(authorMemberId)

//        회식 저장
        val createdGatheringV2: GatheringV2 =
            gatheringV2PersistencePort.save(
                gatheringV2 = gatheringV2,
                authorMember = authorMember,
            )

//        태그로 지정된 멤버 초대
        if (gatheringV2InviteTags.isEmpty()) {
            // 초대 태그가 없으면 회식만 생성하고 초대는 생성하지 않음
            return
        }

        // 초대 태그 저장
        gatheringV2InviteTags.forEach { tag ->
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

        val inviteeMemberIds: List<MemberId> = gatheringV2InviteTags.flatMap { tag ->
            memberQueryUseCase.findAllMemberIdsByCohortIdAndAuthorityId(
                tag.cohortId,
                tag.authorityId,
            )
        }.distinct()

        // 태그에 해당하는 멤버가 없으면 초대를 생성하지 않음
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
}
