package com.server.dpmcore.gathering.gathering.domain.port

import com.server.dpmcore.gathering.gathering.domain.model.Gathering

interface GatheringRepositoryPort {
    fun findGatheringById(id: Long): Gathering

    fun save(gathering: Gathering): Gathering
}
