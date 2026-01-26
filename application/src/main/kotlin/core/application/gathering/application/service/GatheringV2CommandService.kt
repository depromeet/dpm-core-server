package core.application.gathering.application.service

import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.member.application.exception.MemberNotFoundException
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.enums.GatheringV2InviteTag
import core.domain.gathering.port.inbound.GatheringV2CommandUseCase
import core.domain.gathering.port.inbound.GatheringV2InviteeCommandUseCase
import core.domain.gathering.port.outbound.GatheringV2PersistencePort
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.MemberQueryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GatheringV2CommandService(
    val gatheringV2PersistencePort: GatheringV2PersistencePort,
    val gatheringV2InviteeCommandUseCase: GatheringV2InviteeCommandUseCase,
    val memberQueryUseCase: MemberQueryUseCase,
) : GatheringV2CommandUseCase {
    @Transactional
    override fun createGatheringV2(
        gatheringV2: GatheringV2,
//        TODO 준원 : 태그 사용해서 초대하는 기능 구현 필요
        gatheringV2InviteTags: List<GatheringV2InviteTag>,
    ) {
//        회식 저장
        val createdGatheringV2: GatheringV2 = gatheringV2PersistencePort.save(gatheringV2)

//        멤버 전원 초대
        val inviteeMembers: List<Member> = memberQueryUseCase.getAll()

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
                member = inviteeMember,
            )
        }
    }
}
