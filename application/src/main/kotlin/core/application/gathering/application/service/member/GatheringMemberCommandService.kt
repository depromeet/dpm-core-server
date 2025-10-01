package core.application.gathering.application.service.member

import core.application.gathering.application.validator.GatheringMemberValidator
import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.aggregate.GatheringMember
import core.domain.gathering.port.outbound.GatheringMemberPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GatheringMemberCommandService(
    private val gatheringMemberPersistencePort: GatheringMemberPersistencePort,
    private val gatheringMemberValidator: GatheringMemberValidator,
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

    fun markAsJoined(gatheringMember: GatheringMember) {
        gatheringMemberPersistencePort.updateIsJoinedById(gatheringMember)
    }

    fun markAsGatheringParticipationSubmitConfirm(gatheringMember: GatheringMember) {
        gatheringMemberValidator.checkParticipationIsSubmitted(gatheringMember)
        gatheringMemberPersistencePort.updateIsInvitationSubmittedById(gatheringMember.memberId.value)
    }

    fun updateDepositAndMemo(
        gatheringMember: GatheringMember,
        isDeposit: Boolean,
        memo: String?,
    ) {
        gatheringMemberPersistencePort.updateDepositAndMemoById(gatheringMember, isDeposit, memo)
    }
}
