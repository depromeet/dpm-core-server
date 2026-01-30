package core.application.announcement.presentation.controller

import core.application.announcement.application.service.AnnouncementCommandService
import core.application.announcement.presentation.request.CreateAnnouncementRequest
import core.application.common.exception.CustomResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.member.vo.MemberId
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
            startAt = createAnnouncementRequest.startAt,
            dueAt = createAnnouncementRequest.dueAt,
            scheduledAt = createAnnouncementRequest.scheduledAt,
            shouldSendNotification = createAnnouncementRequest.shouldSendNotification,
        )
        return CustomResponse.ok()
    }
}
