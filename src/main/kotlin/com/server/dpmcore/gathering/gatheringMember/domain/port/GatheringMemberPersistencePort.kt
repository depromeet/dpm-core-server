package com.server.dpmcore.gathering.gatheringMember.domain.port

import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember

interface GatheringMemberPersistencePort {
    fun save(gatheringMember: GatheringMember)
}
