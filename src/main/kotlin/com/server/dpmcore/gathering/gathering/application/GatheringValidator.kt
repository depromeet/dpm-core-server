package com.server.dpmcore.gathering.gathering.application

import com.server.dpmcore.gathering.gathering.application.exception.GatheringIdRequiredException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import org.springframework.stereotype.Component

@Component
class GatheringValidator {
    fun validateGatheringIdIsNotNull(gathering: Gathering): GatheringId {
        return gathering.id ?: throw GatheringIdRequiredException()
    }
}
