package core.application.gathering.application.validator

import core.application.gathering.application.exception.GatheringIdRequiredException
import core.domain.gathering.aggregate.Gathering
import core.domain.gathering.vo.GatheringId
import org.springframework.stereotype.Component

@Component
class GatheringValidator {
    fun validateGatheringIdIsNotNull(gathering: Gathering): GatheringId {
        return gathering.id ?: throw GatheringIdRequiredException()
    }
}
