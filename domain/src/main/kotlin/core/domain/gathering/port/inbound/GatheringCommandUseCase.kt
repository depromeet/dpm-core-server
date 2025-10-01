package core.domain.gathering.port.inbound

import core.domain.authority.vo.AuthorityId
import core.domain.bill.port.inbound.command.UpdateMemberDepositCommand
import core.domain.bill.port.inbound.command.UpdateMemberListDepositCommand
import core.domain.bill.vo.BillId
import core.domain.gathering.aggregate.GatheringReceipt
import core.domain.gathering.port.inbound.command.GatheringCreateCommand
import core.domain.gathering.port.inbound.command.JoinGatheringCommand
import core.domain.gathering.vo.GatheringId
import core.domain.member.vo.MemberId

interface GatheringCommandUseCase {
    fun saveAllGatherings(
        commands: List<GatheringCreateCommand>,
        invitedAuthorityIds: List<AuthorityId>,
        billId: BillId,
    )

    fun updateGatheringReceiptSplitAmount(receipt: GatheringReceipt): Boolean

    fun markAsCheckedEachGatheringMember(
        gatheringIds: List<GatheringId>,
        memberId: MemberId,
    )

    fun markAsJoinedEachGatheringMember(
        billId: BillId,
        command: List<JoinGatheringCommand>,
        memberId: MemberId,
    )

    fun submitBillParticipationConfirmEachGathering(
        billId: BillId,
        memberId: MemberId,
    )

    fun updateMemberDeposit(command: UpdateMemberDepositCommand)

    fun updateMemberListDeposit(command: UpdateMemberListDepositCommand)
}
