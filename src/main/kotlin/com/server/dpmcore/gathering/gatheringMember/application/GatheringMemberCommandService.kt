package com.server.dpmcore.gathering.gatheringMember.application

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
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
        gatheringMember.markAsChecked()
        gatheringMemberPersistencePort.updateGatheringMemberById(gatheringMember)
    }

    fun markAsJoined(
        gatheringMember: GatheringMember,
        isJoined: Boolean,
    ) {
        gatheringMember.markAsJoined(isJoined)
        gatheringMemberPersistencePort.updateGatheringMemberById(gatheringMember)
    }

    fun gatheringParticipationConfirm(gatheringMember: GatheringMember) {
        gatheringMember.gatheringParticipationConfirm()
        gatheringMemberPersistencePort.gatheringParticipationConfirm(gatheringMember)
    }
}
