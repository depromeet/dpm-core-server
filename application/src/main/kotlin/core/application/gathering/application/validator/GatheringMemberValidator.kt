package core.application.gathering.application.validator

import core.application.gathering.application.exception.GatheringNotParticipantMemberException
import core.application.gathering.application.exception.member.AlreadySubmittedInvitationException
import core.domain.gathering.aggregate.GatheringMember
import core.domain.gathering.vo.GatheringId
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
