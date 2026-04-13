package core.application.announcement.presentation.controller

import core.application.announcement.application.service.AnnouncementCommandService
import core.application.announcement.presentation.request.CreateAnnouncementRequest
import core.application.announcement.presentation.request.RemindNotificationToMembersRequest
import core.application.announcement.presentation.request.UpdateAnnouncementRequest
import core.application.announcement.presentation.request.UpdateSubmitStatusRequest
import core.application.common.converter.TimeMapper.localDateTimeToInstant
import core.application.common.exception.CustomResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import org.springframework.web.bind.annotation.DeleteMapping
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
            submitType = createAnnouncementRequest.assignment?.submitType,
            title = createAnnouncementRequest.title,
            content = createAnnouncementRequest.content,
            submitLink = createAnnouncementRequest.assignment?.submitLink,
            startAt = localDateTimeToInstant(createAnnouncementRequest.assignment?.startAt),
            dueAt = localDateTimeToInstant(createAnnouncementRequest.assignment?.dueAt),
            scheduledAt = localDateTimeToInstant(createAnnouncementRequest.scheduledAt),
            shouldSendNotification = createAnnouncementRequest.shouldSendNotification,
        )
        return CustomResponse.ok()
    }

    @PostMapping("/{announcementId}/mark-as-read")
    override fun markAsRead(
        @CurrentMemberId
        memberId: MemberId,
        @PathVariable
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
            assignmentScore = updateSubmitStatusRequest.assignmentScore,
        )
        return CustomResponse.ok()
    }

    @DeleteMapping("/{announcementId}")
    override fun deleteAnnouncement(
        @PathVariable
        announcementId: AnnouncementId,
        @CurrentMemberId
        memberId: MemberId,
    ): CustomResponse<Void> {
        announcementCommandService.delete(announcementId)
        return CustomResponse.ok()
    }

    @PatchMapping("/{announcementId}")
    override fun updateAnnouncement(
        @PathVariable
        announcementId: AnnouncementId,
        @RequestBody
        updateAnnouncementRequest: UpdateAnnouncementRequest,
        @CurrentMemberId
        memberId: MemberId,
    ): CustomResponse<Void> {
        announcementCommandService.update(
            announcementId = announcementId,
            announcementType = updateAnnouncementRequest.announcementType,
            submitType = updateAnnouncementRequest.assignment?.submitType,
            title = updateAnnouncementRequest.title,
            content = updateAnnouncementRequest.content,
            submitLink = updateAnnouncementRequest.assignment?.submitLink,
            startAt = localDateTimeToInstant(updateAnnouncementRequest.assignment?.startAt),
            dueAt = localDateTimeToInstant(updateAnnouncementRequest.assignment?.dueAt),
            scheduledAt = localDateTimeToInstant(updateAnnouncementRequest.scheduledAt),
            shouldSendNotification = updateAnnouncementRequest.shouldSendNotification,
        )
        return CustomResponse.ok()
    }

    @PostMapping("/{announcementId}/remind-notification")
    override fun remindNotification(
        @PathVariable announcementId: AnnouncementId,
    ): CustomResponse<Void> {
        announcementCommandService.remindNotification(announcementId)
        return CustomResponse.ok()
    }

    @PostMapping("/{announcementId}/remind-notification-to-members")
    override fun remindNotificationToMembers(
        @PathVariable announcementId: AnnouncementId,
        @RequestBody remindNotificationToMembersRequest: RemindNotificationToMembersRequest,
    ): CustomResponse<Void> {
        announcementCommandService.remindNotificationToMembers(
            announcementId = announcementId,
            memberIds = remindNotificationToMembersRequest.memberIds,
        )
        return CustomResponse.ok()
    }
}
