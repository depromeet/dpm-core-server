package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringCategory
import com.server.dpmcore.gathering.gathering.domain.port.GatheringRepositoryPort
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class GatheringCommandService(
    private val gatheringRepositoryPort: GatheringRepositoryPort,
) {
    fun save(
        title: String,
        description: String,
        heldAt: LocalDateTime,
        category: String,
        hostUserId: Long,
        roundNumber: Int,
        gatheringMembers: MutableList<GatheringMember>,
    ): Gathering {
        if (gatheringMembers.isNotEmpty()) {
//            TODO : 회식 참여 멤버 저장 로직 추가
        }
        return gatheringRepositoryPort.save(
            Gathering(
                title = title,
                description = description,
                heldAt = heldAt.atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant(),
                category = GatheringCategory.valueOf(category),
                hostUserId = MemberId(hostUserId),
                roundNumber = roundNumber,
            ),
        )
    }
}
