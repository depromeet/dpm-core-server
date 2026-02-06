package core.application.gathering.application.service

import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.gathering.application.exception.member.GatheringMemberNotFoundException
import core.application.gathering.presentation.response.GatheringV2DetailResponse
import core.application.gathering.presentation.response.GatheringV2ListResponse
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.port.inbound.GatheringV2InviteeQueryUseCase
import core.domain.gathering.port.inbound.GatheringV2QueryUseCase
import core.domain.gathering.port.outbound.GatheringV2PersistencePort
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GatheringV2QueryService(
    val gatheringV2PersistencePort: GatheringV2PersistencePort,
    val gatheringV2InviteeQueryUseCase: GatheringV2InviteeQueryUseCase,
) : GatheringV2QueryUseCase {
    fun getAllGatherings(memberId: MemberId): List<GatheringV2ListResponse> {
        val gatheringV2s: List<GatheringV2> = gatheringV2PersistencePort.findAll()

        return gatheringV2s.map { gatheringV2 ->
            val invitees =
                gatheringV2InviteeQueryUseCase.getInviteesByGatheringV2Id(
                    gatheringV2.id ?: throw GatheringNotFoundException(),
                )
            val rsvpStatus = invitees.find { it.memberId == memberId }?.rsvpStatus
            val isAttended = invitees.find { it.memberId == memberId }?.isAttended

            GatheringV2ListResponse.of(
                gatheringV2 = gatheringV2,
                memberId = memberId,
                rsvpStatus = rsvpStatus,
                isAttended = isAttended,
                isRsvpGoingCount = invitees.count { it.isRsvpGoing() },
                isAttendedCount = invitees.count { it.isAttended == true },
                inviteeCount = invitees.count(),
            )
        }
    }

    fun getGatheringV2Detail(
        gatheringV2Id: GatheringV2Id,
        memberId: MemberId,
    ): GatheringV2DetailResponse {
        val gatheringV2: GatheringV2 =
            gatheringV2PersistencePort.findById(gatheringV2Id)
                ?: throw GatheringNotFoundException()

        val invitees =
            gatheringV2InviteeQueryUseCase.getInviteesByGatheringV2Id(
                gatheringV2.id ?: throw GatheringNotFoundException(),
            )

        val myInvitee =
            invitees.find { it.memberId == memberId }
                ?: throw GatheringMemberNotFoundException()

        return GatheringV2DetailResponse.of(
            gatheringV2 = gatheringV2,
            isOwner = gatheringV2.authorMemberId == memberId,
            rsvpStatus = myInvitee.isRsvpGoing(),
            isAttended = myInvitee.isAttended,
            isRsvpGoingCount = invitees.count { it.isRsvpGoing() },
            inviteeCount = invitees.size,
            attendanceCount = invitees.count { it.isAttended == true },
        )
    }
}
