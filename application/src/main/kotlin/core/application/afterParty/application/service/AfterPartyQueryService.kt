package core.application.afterParty.application.service

import core.application.afterParty.application.exception.AfterPartyNotFoundException
import core.application.afterParty.presentation.response.AfterPartyDetailResponse
import core.application.afterParty.presentation.response.AfterPartyInviteTagListResponse
import core.application.afterParty.presentation.response.AfterPartyInviteTagNameResponse
import core.application.afterParty.presentation.response.AfterPartyListResponse
import core.application.cohort.application.service.CohortQueryService
import core.application.gathering.presentation.response.GatheringV2DetailResponse
import core.application.gathering.presentation.response.GatheringV2InviteTagListResponse
import core.application.gathering.presentation.response.GatheringV2InviteTagNameResponse
import core.application.gathering.presentation.response.GatheringV2ListResponse
import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.port.inbound.AfterPartyInviteTagQueryUseCase
import core.domain.afterParty.port.inbound.AfterPartyInviteeQueryUseCase
import core.domain.afterParty.port.inbound.AfterPartyQueryUseCase
import core.domain.afterParty.port.outbound.AfterPartyPersistencePort
import core.domain.afterParty.vo.AfterPartyId
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AfterPartyQueryService(
    val afterPartyPersistencePort: AfterPartyPersistencePort,
    val afterPartyInviteeQueryUseCase: AfterPartyInviteeQueryUseCase,
    val afterPartyInviteTagQueryUseCase: AfterPartyInviteTagQueryUseCase,
    val cohortQueryService: CohortQueryService,
) : AfterPartyQueryUseCase {
    fun getAfterPartyInviteTags(): AfterPartyInviteTagListResponse =
        AfterPartyInviteTagListResponse(
            inviteTags =
                buildLatestInviteTags().map {
                    AfterPartyInviteTagNameResponse.from(it)
                },
        )

    @Deprecated("GatheringV2에서만 사용되는 메서드입니다. GatheringV2 삭제 후 제거 예정입니다.")
    fun getGatheringV2InviteTags(): GatheringV2InviteTagListResponse =
        GatheringV2InviteTagListResponse(
            inviteTags =
                buildLatestInviteTags().map {
                    GatheringV2InviteTagNameResponse.from(it)
                },
        )

    fun getAllAfterPartys(
        memberId: MemberId,
        inviteTagCohortId: Long? = null,
        inviteTagAuthorityId: Long? = null,
    ): List<AfterPartyListResponse> {
        val afterParties: List<AfterParty> =
            if (inviteTagCohortId == null && inviteTagAuthorityId == null) {
                afterPartyPersistencePort.findAll()
            } else {
                val cohortId = inviteTagCohortId?.let { CohortId(it) }
                afterPartyPersistencePort.findByInviteTagFilters(
                    cohortId = cohortId,
                    authorityId = inviteTagAuthorityId,
                )
            }

        return afterParties
            .map { afterParty ->
                val invitees =
                    afterPartyInviteeQueryUseCase.getInviteesByAfterPartyId(
                        afterParty.id ?: throw AfterPartyNotFoundException(),
                    )
                val rsvpStatus = invitees.find { it.memberId == memberId }?.rsvpStatus
                val isAttended = invitees.find { it.memberId == memberId }?.isAttended

                AfterPartyListResponse.of(
                    afterParty = afterParty,
                    memberId = memberId,
                    rsvpStatus = rsvpStatus,
                    isAttended = isAttended,
                    isRsvpGoingCount = invitees.count { it.isRsvpGoing() },
                    isAttendedCount = invitees.count { it.isAttended == true },
                    inviteeCount = invitees.count(),
                )
            }.sortedByDescending { it.createdAt }
    }

    @Deprecated("GatheringV2에서만 사용되는 메서드입니다. GatheringV2 삭제 후 제거 예정입니다.")
    fun getAllGatherings(
        memberId: MemberId,
        inviteTagCohortId: Long? = null,
        inviteTagAuthorityId: Long? = null,
    ): List<GatheringV2ListResponse> {
        val afterParties: List<AfterParty> =
            if (inviteTagCohortId == null && inviteTagAuthorityId == null) {
                afterPartyPersistencePort.findAll()
            } else {
                val cohortId = inviteTagCohortId?.let { CohortId(it) }
                afterPartyPersistencePort.findByInviteTagFilters(
                    cohortId = cohortId,
                    authorityId = inviteTagAuthorityId,
                )
            }

        return afterParties
            .map { afterParty ->
                val invitees =
                    afterPartyInviteeQueryUseCase.getInviteesByAfterPartyId(
                        afterParty.id ?: throw AfterPartyNotFoundException(),
                    )
                val rsvpStatus = invitees.find { it.memberId == memberId }?.rsvpStatus
                val isAttended = invitees.find { it.memberId == memberId }?.isAttended

                GatheringV2ListResponse.of(
                    afterParty = afterParty,
                    memberId = memberId,
                    rsvpStatus = rsvpStatus,
                    isAttended = isAttended,
                    isRsvpGoingCount = invitees.count { it.isRsvpGoing() },
                    isAttendedCount = invitees.count { it.isAttended == true },
                    inviteeCount = invitees.count(),
                )
            }.sortedByDescending { it.createdAt }
    }

    fun getAfterPartyDetail(
        afterPartyId: AfterPartyId,
        memberId: MemberId,
    ): AfterPartyDetailResponse {
        val afterParty: AfterParty =
            afterPartyPersistencePort.findById(afterPartyId)
                ?: throw AfterPartyNotFoundException()

        val invitees =
            afterPartyInviteeQueryUseCase.getInviteesByAfterPartyId(
                afterParty.id ?: throw AfterPartyNotFoundException(),
            )

        // Get actual invite tags for this afterParty
        val inviteTags: List<AfterPartyInviteTag> =
            afterPartyInviteTagQueryUseCase.findByAfterPartyId(afterPartyId)

        val myInvitee =
            invitees.find { it.memberId == memberId }

        return AfterPartyDetailResponse.of(
            afterParty = afterParty,
            isOwner = afterParty.authorMemberId == memberId,
            rsvpStatus = myInvitee?.rsvpStatus,
            isAttended = myInvitee?.isAttended,
            submitRsvpCount = invitees.count { it.rsvpStatus != null },
            rsvpGoingCount = invitees.count { it.isRsvpGoing() },
            notRsvpGoingCount = invitees.count { it.rsvpStatus == false },
            inviteeCount = invitees.size,
            attendanceCount = invitees.count { it.isAttended == true },
            isClosed = afterParty.isClosed(),
            inviteTags = inviteTags,
        )
    }

    @Deprecated("GatheringV2에서만 사용되는 메서드입니다. GatheringV2 삭제 후 제거 예정입니다.")
    fun getGatheringV2Detail(
        afterPartyId: AfterPartyId,
        memberId: MemberId,
    ): GatheringV2DetailResponse {
        val afterParty: AfterParty =
            afterPartyPersistencePort.findById(afterPartyId)
                ?: throw AfterPartyNotFoundException()

        val invitees =
            afterPartyInviteeQueryUseCase.getInviteesByAfterPartyId(
                afterParty.id ?: throw AfterPartyNotFoundException(),
            )

        // Get actual invite tags for this afterParty
        val inviteTags: List<AfterPartyInviteTag> =
            afterPartyInviteTagQueryUseCase.findByAfterPartyId(afterPartyId)

        val myInvitee =
            invitees.find { it.memberId == memberId }

        return GatheringV2DetailResponse.of(
            afterParty = afterParty,
            isOwner = afterParty.authorMemberId == memberId,
            rsvpStatus = myInvitee?.rsvpStatus,
            isAttended = myInvitee?.isAttended,
            submitRsvpCount = invitees.count { it.rsvpStatus != null },
            rsvpGoingCount = invitees.count { it.isRsvpGoing() },
            notRsvpGoingCount = invitees.count { it.rsvpStatus == false },
            inviteeCount = invitees.size,
            attendanceCount = invitees.count { it.isAttended == true },
            isClosed = afterParty.isClosed(),
            inviteTags = inviteTags,
        )
    }

    private fun buildLatestInviteTags(): List<AfterPartyInviteTag> {
        val latestCohort = cohortQueryService.getLatestCohort()
        val afterPartyId = AfterPartyId(0L)
        return listOf(
            AfterPartyInviteTag.create(
                afterPartyId = afterPartyId,
                cohortId = latestCohort.id ?: throw AfterPartyNotFoundException(),
                authorityId = DEEPER_AUTHORITY_ID,
                tagName = "${latestCohort.value}기 디퍼",
            ),
            AfterPartyInviteTag.create(
                afterPartyId = afterPartyId,
                cohortId = latestCohort.id,
                authorityId = ORGANIZER_AUTHORITY_ID,
                tagName = "${latestCohort.value}기 운영진",
            ),
        )
    }

    companion object {
        private const val DEEPER_AUTHORITY_ID = 1L
        private const val ORGANIZER_AUTHORITY_ID = 2L
    }
}
