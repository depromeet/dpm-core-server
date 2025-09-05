package com.server.dpmcore.gathering.gatheringMember.application

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringDepositCommand
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringJoinCommand
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.outbound.GatheringMemberPersistencePort
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GatheringMemberCommandService(
    private val gatheringMemberPersistencePort: GatheringMemberPersistencePort,
) {
    fun saveEachGatheringMembers(
        memberIds: List<MemberId>,
        gathering: Gathering,
    ) = memberIds.map { memberId ->
        gatheringMemberPersistencePort.save(GatheringMember.create(gathering.id!!, memberId), gathering)
    }

    fun markAsChecked(gatheringMember: GatheringMember) {
        gatheringMemberPersistencePort.updateIsViewedById(gatheringMember.memberId.value)
    }

    fun markAsJoined(command: GatheringJoinCommand) {
        gatheringMemberPersistencePort.updateIsJoinedById(command)
    }

    fun markAsGatheringParticipationSubmitConfirm(gatheringMember: GatheringMember) {
        gatheringMember.checkParticipationIsSubmitted()
        gatheringMemberPersistencePort.updateIsInvitationSubmittedById(gatheringMember.memberId.value)
    }

    fun updateDepositAndMemo(command: GatheringDepositCommand) {
        gatheringMemberPersistencePort.updateDepositAndMemoById(command)
    }
}
