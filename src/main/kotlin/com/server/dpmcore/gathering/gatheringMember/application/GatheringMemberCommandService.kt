package com.server.dpmcore.gathering.gatheringMember.application

import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.port.GatheringMemberPersistencePort
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class GatheringMemberCommandService(
    private val gatheringMemberPersistencePort: GatheringMemberPersistencePort,
) {
    fun save(
        memberId: Long,
        isChecked: Boolean,
        isJoined: Boolean,
        completedAt: Instant,
    ) {
        gatheringMemberPersistencePort.save(
            GatheringMember(
                memberId = MemberId(memberId),
//                TODO 준원 : gathering 받아오기
                isChecked = isChecked,
                isJoined = isJoined,
                completedAt = completedAt,
            ),
        )
    }

    fun save(gatheringMember: GatheringMember): GatheringMember = gatheringMemberPersistencePort.save(gatheringMember)

//        TODO : 벌크 insert 로직 추가
    fun save(gatheringMembers: MutableList<GatheringMember>): MutableList<GatheringMember> {
        if (gatheringMembers.isEmpty()) throw BillException.GatheringMembersRequiredException()
        return gatheringMembers
            .map { gatheringMember ->
                gatheringMemberPersistencePort.save(gatheringMember)
            }.toMutableList()
    }
}
