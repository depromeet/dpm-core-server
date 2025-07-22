package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.BillQueryUseCase
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.inbound.GatheringCreateUseCase
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringCreateCommand
import com.server.dpmcore.gathering.gathering.domain.port.outbound.GatheringPersistencePort
import com.server.dpmcore.gathering.gatheringMember.application.GatheringMemberCommandService
import com.server.dpmcore.gathering.gatheringReceipt.application.ReceiptCommandService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GatheringCommandService(
    private val gatheringPersistencePort: GatheringPersistencePort,
    private val gatheringMemberCommandService: GatheringMemberCommandService,
    private val receiptCommandService: ReceiptCommandService,
    private val billQueryUseCase: BillQueryUseCase,
) : GatheringCreateUseCase {
    /*
    fun save(
        bill: Bill,
        gathering: Gathering,
    ): Gathering {
        val savedGathering =
            gatheringPersistencePort.save(
//                TODO : 팩토리 도입하기
                bill,
                Gathering(
                    title = gathering.title,
                    description = gathering.description,
                    heldAt = gathering.heldAt.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                    category = gathering.category,
                    hostUserId = gathering.hostUserId,
                    roundNumber = gathering.roundNumber,
                    billId = gathering.billId,
                ),
            )

        gathering.gatheringMembers.map { gatheringMember ->
            gatheringMember.gatheringId = savedGathering.id
        }
        if (gathering.gatheringMembers.isNotEmpty()) {
//            TODO 준원 : 회식 참여 멤버 저장 로직 추가
            savedGathering.gatheringMembers = gatheringMemberCommandService.save(gathering.gatheringMembers)
        }
//        TODO : 회식 영수증 저장 로직 추가
        if (gathering.receipt != null) {
            gathering.receipt!!.gathering = savedGathering
//            TODO : public setter말고 다른 방법 적용하기
            savedGathering.receipt = receiptCommandService.save(gathering.receipt!!)
        }
        return savedGathering
    }

    fun save(
        bill: Bill,
        gatherings: MutableList<Gathering>,
    ): MutableList<Gathering> {
        if (gatherings.isEmpty()) {
            throw BillException.GatheringRequiredException()
        }
//        TODO : 벌크 insert 로직 추가
        return gatherings.map { save(bill, it) }.toMutableList()
    }


     */

    override fun saveAllGatherings(
        commands: List<GatheringCreateCommand>,
        billId: BillId,
    ) {
        try {
            val bill = billQueryUseCase.getById(billId)
            commands.forEach {
                val gathering = gatheringPersistencePort.save(bill, Gathering.create(it))
                receiptCommandService.saveReceiptDetails(it.receipt, gathering)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
