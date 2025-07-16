package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.GatheringRepositoryPort
import com.server.dpmcore.gathering.gatheringMember.application.GatheringMemberCommandService
import com.server.dpmcore.gathering.gatheringReceipt.application.ReceiptCommandService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId

@Service
@Transactional
class GatheringCommandService(
    private val gatheringRepositoryPort: GatheringRepositoryPort,
    private val gatheringMemberCommandService: GatheringMemberCommandService,
    private val receiptCommandService: ReceiptCommandService,
) {
    fun save(gathering: Gathering): Gathering {
        try {
            println("영수증 1111: ${gathering.receipt}")
            val gatheringResult =
                gatheringRepositoryPort.save(
                    Gathering(
                        title = gathering.title,
                        description = gathering.description,
                        heldAt = gathering.heldAt.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                        category = gathering.category,
                        hostUserId = gathering.hostUserId,
                        roundNumber = gathering.roundNumber,
                        bill = gathering.bill,
                    ),
                )

            gathering.gatheringMembers.map { gatheringMember ->
                gatheringMember.gathering = gatheringResult
            }
            if (gathering.gatheringMembers.isNotEmpty()) {
//            TODO 준원 : 회식 참여 멤버 저장 로직 추가
                gatheringResult.gatheringMembers = gatheringMemberCommandService.save(gathering.gatheringMembers)
            }
//        TODO : 회식 영수증 저장 로직 추가
            if (gathering.receipt != null) {
                gathering.receipt!!.gathering = gatheringResult
                println("영수증 저장 로직 실행 2222: ${gathering.receipt}")
                gatheringResult.receipt = receiptCommandService.save(gathering.receipt!!)
            }
            return gatheringResult
        } catch (ex: Exception) {
            throw IllegalArgumentException(
                "영수증 저장 중 오류뜸: ${ex.stackTrace.joinToString("\n") { it.toString() }}",
                ex,
            )
        }
    }

    fun save(gatherings: MutableList<Gathering>): MutableList<Gathering> {
        if (gatherings.isEmpty()) {
            throw IllegalArgumentException("회식은 필수로 존재해야합니다.")
        }
        return gatherings.map { save(it) }.toMutableList()
    }
}
