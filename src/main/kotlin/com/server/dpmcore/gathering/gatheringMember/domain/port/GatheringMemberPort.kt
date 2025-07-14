package com.server.dpmcore.gathering.gatheringMember.domain.port

import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember


interface GatheringMemberPort {
    fun save(gatheringMember: GatheringMember): GatheringMember
}
