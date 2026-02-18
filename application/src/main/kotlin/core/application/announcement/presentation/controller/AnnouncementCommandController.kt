package core.application.announcement.presentation.controller

import core.application.announcement.application.service.AnnouncementCommandService
import core.application.announcement.presentation.request.CreateAnnouncementRequest
import core.application.announcement.presentation.request.UpdateSubmitStatusRequest
import core.application.common.exception.CustomResponse
import core.application.security.annotation.CurrentMemberId
import core.application.session.presentation.mapper.TimeMapper.localDateTimeToInstant
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/announcements")
class AnnouncementCommandController(
    val announcementCommandService: AnnouncementCommandService,
) : AnnouncementCommandApi {
    @PostMapping
    override fun createAnnouncement(
        @RequestBody
        createAnnouncementRequest: CreateAnnouncementRequest,
        @CurrentMemberId
        memberId: MemberId,
    ): CustomResponse<Void> {
        announcementCommandService.create(
            authorId = memberId,
            announcementType = createAnnouncementRequest.announcementType,
            submitType = createAnnouncementRequest.submitType,
            title = createAnnouncementRequest.title,
            content = createAnnouncementRequest.content,
            submitLink = createAnnouncementRequest.submitLink,
            startAt = localDateTimeToInstant(createAnnouncementRequest.startAt),
            dueAt = localDateTimeToInstant(createAnnouncementRequest.dueAt),
            scheduledAt = createAnnouncementRequest.scheduledAt?.let { localDateTimeToInstant(it) },
            shouldSendNotification = createAnnouncementRequest.shouldSendNotification,
        )
        return CustomResponse.ok()
    }

    @PostMapping("/{announcementId}/mark-as-read")
    override fun markAsRead(
        @CurrentMemberId
        memberId: MemberId,
        announcementId: AnnouncementId,
    ): CustomResponse<Void> {
        announcementCommandService.markAsRead(
            memberId = memberId,
            announcementId = announcementId,
        )
        return CustomResponse.ok()
    }

    @PatchMapping("/{announcementId}/assignment-status")
    override fun updateSubmitStatus(
        @PathVariable
        announcementId: AnnouncementId,
        @RequestBody
        updateSubmitStatusRequest: UpdateSubmitStatusRequest,
    ): CustomResponse<Void> {
        announcementCommandService.updateSubmitStatus(
            announcementId = announcementId,
            memberIds = updateSubmitStatusRequest.memberIds,
            submitStatus = updateSubmitStatusRequest.submitStatus,
        )
        return CustomResponse.ok()
    }
}
