package core.application.gathering.application.service

import core.application.gathering.application.exception.GatheringNotFoundException
import core.application.gathering.presentation.response.GatheringV2DetailResponse
import core.application.gathering.presentation.response.GatheringV2InviteTagListResponse
import core.application.gathering.presentation.response.GatheringV2InviteTagNameResponse
import core.application.gathering.presentation.response.GatheringV2ListResponse
import core.domain.cohort.vo.CohortId
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2InviteTag
import core.domain.gathering.port.inbound.GatheringV2InviteTagQueryUseCase
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
    val gatheringV2InviteTagQueryUseCase: GatheringV2InviteTagQueryUseCase,
) : GatheringV2QueryUseCase {
    fun getGatheringV2InviteTags(): GatheringV2InviteTagListResponse {
        val inviteTags = gatheringV2InviteTagQueryUseCase.findAllDistinct()
        return GatheringV2InviteTagListResponse(
            inviteTags = inviteTags.map { GatheringV2InviteTagNameResponse.from(it) },
        )
    }

    fun getAllGatherings(
        memberId: MemberId,
        inviteTagCohortId: Long? = null,
        inviteTagAuthorityId: Long? = null,
    ): List<GatheringV2ListResponse> {
        val gatheringV2s: List<GatheringV2> =
            if (inviteTagCohortId == null && inviteTagAuthorityId == null) {
                gatheringV2PersistencePort.findAll()
            } else {
                val cohortId = inviteTagCohortId?.let { CohortId(it) }
                gatheringV2PersistencePort.findByInviteTagFilters(
                    cohortId = cohortId,
                    authorityId = inviteTagAuthorityId,
                )
            }

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

        // Get actual invite tags for this gathering
        val inviteTags: List<GatheringV2InviteTag> =
            gatheringV2InviteTagQueryUseCase.findByGatheringId(gatheringV2Id)

        val myInvitee =
            invitees.find { it.memberId == memberId }

        return GatheringV2DetailResponse.of(
            gatheringV2 = gatheringV2,
            isOwner = gatheringV2.authorMemberId == memberId,
            rsvpStatus = myInvitee?.rsvpStatus,
            isAttended = myInvitee?.isAttended,
            isRsvpGoingCount = invitees.count { it.isRsvpGoing() },
            inviteeCount = invitees.size,
            attendanceCount = invitees.count { it.isAttended == true },
            isClosed = gatheringV2.isClosed(),
            inviteTags = inviteTags,
        )
    }
}
