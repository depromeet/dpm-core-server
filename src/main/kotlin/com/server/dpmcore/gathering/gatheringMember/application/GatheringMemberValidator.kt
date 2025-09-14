package com.server.dpmcore.gathering.gatheringMember.application

import com.server.dpmcore.gathering.gathering.application.exception.GatheringNotParticipantMemberException
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.application.exception.AlreadySubmittedInvitationException
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import org.springframework.stereotype.Component

@Component
class GatheringMemberValidator {
    fun checkParticipationIsSubmitted(gatheringMember: GatheringMember) {
        if (gatheringMember.isInvitationSubmitted) throw AlreadySubmittedInvitationException()
    }

    fun validateGatheringIdMatches(
        gatheringMember: GatheringMember,
        gatheringId: GatheringId,
    ) {
        if (!gatheringMember.isGatheringIdMatches(gatheringId)) throw GatheringNotParticipantMemberException()
    }
}
