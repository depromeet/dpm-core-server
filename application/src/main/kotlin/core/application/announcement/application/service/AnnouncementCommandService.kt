package core.application.announcement.application.service

import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.aggregate.AnnouncementAssignment
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.port.inbound.AnnouncementCommandUseCase
import core.domain.announcement.port.outbound.AnnouncementAssignmentPersistencePort
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.domain.announcement.port.outbound.AssignmentPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AnnouncementCommandService(
    val announcementCommandPort: AnnouncementPersistencePort,
    val announcementAssignmentPersistencePort: AnnouncementAssignmentPersistencePort,
    val assignmentPersistencePort: AssignmentPersistencePort,
) : AnnouncementCommandUseCase {
    @Transactional
    override fun create(
        authorId: MemberId,
        announcementType: AnnouncementType,
        submitType: SubmitType,
        title: String,
        content: String?,
        submitLink: String?,
        startAt: Instant,
        dueAt: Instant,
        scheduledAt: Instant?,
        shouldSendNotification: Boolean,
    ) {
//        TODO : 스케쥴, 알림 구현
//            scheduledAt: Instant?,
//            shouldSendNotification: Boolean,

//        announcement 작성
        val announcement =
            Announcement.create(
                announcementType = announcementType,
                title = title,
                content = content,
                authorId = authorId,
            )
        val savedAnnouncement: Announcement = announcementCommandPort.save(announcement)

        when (announcementType) {
            AnnouncementType.GENERAL -> {}
            AnnouncementType.ASSIGNMENT -> {
                val assignment: Assignment =
                    Assignment.create(
                        submitType = submitType,
                        startAt = startAt,
                        dueAt = dueAt,
                        submitLink = submitLink,
                    )
                val savedAssignment: Assignment = assignmentPersistencePort.save(assignment)

                val announcementAssignment: AnnouncementAssignment =
                    AnnouncementAssignment.create(
                        announcementId = savedAnnouncement.id!!,
                        assignmentId = savedAssignment.id!!,
                    )
                announcementAssignmentPersistencePort.save(announcementAssignment)
            }
        }
    }
}
