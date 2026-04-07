package core.application.announcement.application.service

import core.application.announcement.application.exception.AnnouncementNotFoundException
import core.application.announcement.application.exception.AnnouncementTypeCannotBeChangedException
import core.application.announcement.application.exception.AssignmentSubmitTypeNotNullException
import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.aggregate.AnnouncementAssignment
import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitStatus
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.event.AnnouncementCreatedEvent
import core.domain.announcement.event.AnnouncementRemindEvent
import core.domain.announcement.port.inbound.AnnouncementAssignmentCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementQueryUseCase
import core.domain.announcement.port.inbound.AnnouncementReadCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementReadQueryUseCase
import core.domain.announcement.port.inbound.AssignmentQueryUseCase
import core.domain.announcement.port.inbound.AssignmentSubmissionCommandUseCase
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.domain.announcement.port.outbound.AssignmentPersistencePort
import core.domain.announcement.vo.AnnouncementId
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.SentAnnouncementNotification
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.inbound.SentAnnouncementNotificationCommandUseCase
import core.domain.notification.vo.SentAnnouncementNotificationId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class AnnouncementCommandService(
    val announcementPersistencePort: AnnouncementPersistencePort,
    val announcementAssignmentCommandUseCase: AnnouncementAssignmentCommandUseCase,
    val assignmentPersistencePort: AssignmentPersistencePort,
    val announcementReadCommandUseCase: AnnouncementReadCommandUseCase,
    val announcementQueryUseCase: AnnouncementQueryUseCase,
    val announcementReadQueryUseCase: AnnouncementReadQueryUseCase,
    val assignmentQueryUseCase: AssignmentQueryUseCase,
    val assignmentSubmissionCommandUseCase: AssignmentSubmissionCommandUseCase,
    val memberQueryUseCase: MemberQueryUseCase,
    val cohortQueryUseCase: CohortQueryUseCase,
    val sentAnnouncementNotificationCommandUseCase: SentAnnouncementNotificationCommandUseCase,
    val eventPublisher: ApplicationEventPublisher,
) : AnnouncementCommandUseCase {
    override fun create(
        authorId: MemberId,
        announcementType: AnnouncementType,
        submitType: SubmitType?,
        title: String,
        content: String?,
        submitLink: String?,
        startAt: Instant?,
        dueAt: Instant?,
        scheduledAt: Instant?,
        shouldSendNotification: Boolean,
    ) {
//        TODO : 공지 예약 구현
//            scheduledAt: Instant?

//        announcement 작성
        val announcement =
            Announcement.create(
                announcementType = announcementType,
                title = title,
                content = content,
                authorId = authorId,
            )
        val savedAnnouncement: Announcement = announcementPersistencePort.save(announcement)

        when (announcementType) {
            AnnouncementType.GENERAL -> {}
            AnnouncementType.ASSIGNMENT -> {
                if (submitType == null) throw AssignmentSubmitTypeNotNullException()
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
                announcementAssignmentCommandUseCase.create(
                    announcementAssignment = announcementAssignment,
                    assignment = savedAssignment,
                )

                // 과제 리마인드 알림 레코드 3개 생성 (24H, 12H, 1H)
                listOf(
                    NotificationMessageType.ASSIGNMENT_DUE_24H,
                    NotificationMessageType.ASSIGNMENT_DUE_12H,
                    NotificationMessageType.ASSIGNMENT_DUE_1H,
                ).forEach { messageType ->
                    sentAnnouncementNotificationCommandUseCase.save(
                        SentAnnouncementNotification(
                            sentAnnouncementNotificationId = SentAnnouncementNotificationId(0L),
                            announcementId = savedAnnouncement.id!!,
                            notificationMessageType = messageType,
                            sentAt = null,
                        ),
                    )
                }
            }
        }

        val memberIds: List<MemberId> =
            memberQueryUseCase.getMemberIdsByCohortId(
                cohortQueryUseCase.getLatestCohortId(),
            )
        announcementReadCommandUseCase.initializeForMembers(
            announcementId = savedAnnouncement.id ?: throw AnnouncementNotFoundException(),
            memberIds = memberIds,
        )

//        TODO : 공지 작성 시점에 기수 별로 공지가 나눠져 있지 않은 상태라서, 일단은 최신 기수에 속한 멤버들에게만 읽음 이력을 초기화. 추후에 공지를 기수 별로 나누거나, 공지 읽음 이력을 기수 별로 나눌 때, 해당 부분도 같이 수정 필요
//        TODO : 게시물 예약 작성 기능
        if (shouldSendNotification) {
            eventPublisher.publishEvent(
                AnnouncementCreatedEvent.of(
                    announcementId = savedAnnouncement.id ?: throw AnnouncementNotFoundException(),
                    announcementType = announcementType,
                    title = title,
                    cohortId = cohortQueryUseCase.getLatestCohortId(),
                ),
            )
        }
    }

    override fun markAsRead(
        memberId: MemberId,
        announcementId: AnnouncementId,
    ) {
        announcementQueryUseCase.getAnnouncementById(announcementId)

        val existedAnnouncement: AnnouncementRead =
            announcementReadQueryUseCase.getByAnnouncementIdAndMemberId(
                announcementId = announcementId,
                memberId = memberId,
            )
        if (!existedAnnouncement.isRead()) {
            announcementReadCommandUseCase.markAsRead(existedAnnouncement)
        }
    }

    override fun updateSubmitStatus(
        announcementId: AnnouncementId,
        memberIds: List<MemberId>,
        submitStatus: SubmitStatus,
        assignmentScore: Int?,
    ) {
        val retrievedAssignment: Assignment =
            assignmentQueryUseCase.getAssignmentByAnnouncementId(announcementId)

        memberIds.forEach { memberId ->
            val memberAssignmentSubmission: AssignmentSubmission =
                assignmentSubmissionCommandUseCase.ensureAssignmentSubmission(
                    assignment = retrievedAssignment,
                    memberId = memberId,
                )
            val updatedAssignmentSubmission: AssignmentSubmission =
                memberAssignmentSubmission.updateSubmitStatus(
                    newSubmitStatus = submitStatus,
                    newScore = assignmentScore,
                )
            assignmentSubmissionCommandUseCase.updateAssignmentSubmission(updatedAssignmentSubmission)
        }
    }

    override fun delete(announcementId: AnnouncementId) {
        val announcement: Announcement = announcementQueryUseCase.getAnnouncementById(announcementId)
        announcement.markAsDeleted()
        announcementPersistencePort.softDeleteByAnnouncement(announcement)
    }

    override fun update(
        announcementId: AnnouncementId,
        announcementType: AnnouncementType,
        submitType: SubmitType?,
        title: String,
        content: String?,
        submitLink: String?,
        startAt: Instant?,
        dueAt: Instant?,
        scheduledAt: Instant?,
        shouldSendNotification: Boolean,
    ) {
        val existingAnnouncement: Announcement = announcementQueryUseCase.getAnnouncementById(announcementId)
        if (existingAnnouncement.announcementType != announcementType) {
            throw AnnouncementTypeCannotBeChangedException()
        }

        existingAnnouncement.update(
            title = title,
            content = content,
        )
        announcementPersistencePort.update(existingAnnouncement)

        when (existingAnnouncement.announcementType) {
            AnnouncementType.GENERAL -> {}
            AnnouncementType.ASSIGNMENT -> {
                val existingAssignment: Assignment =
                    assignmentQueryUseCase.getAssignmentByAnnouncementId(
                        announcementId,
                    )
                existingAssignment.update(
                    submitType = submitType,
                    startAt = startAt,
                    dueAt = dueAt,
                    submitLink = submitLink,
                )

//                TODO : 스케쥴이나 알림 변경 시 적용

                assignmentPersistencePort.save(existingAssignment)
            }
        }
    }

    override fun initializeForNewCohortMember(
        memberId: MemberId,
        cohortId: CohortId,
    ) {
        // TODO : 해당 기수의 공지 읽음 이력, 과제 제출 현황에 추가. 지금은 공지/과제가 기수 별로 나눠져 있지 않은 상태라서, 공지 읽음 이력도 기수 별로 나눠야 할듯
        val announcements: List<Announcement> = announcementQueryUseCase.getAll()
        val announcementIds: List<AnnouncementId> = announcements.map { it.id ?: throw AnnouncementNotFoundException() }

        announcementReadCommandUseCase.initializeForNewCohortMember(
            memberId = memberId,
            announcementIds = announcementIds,
        )

        assignmentSubmissionCommandUseCase.initializeForNewCohortMember(
            memberId = memberId,
            assignments = assignmentQueryUseCase.getAllAssignments(),
        )
    }

    override fun remindNotification(announcementId: AnnouncementId) {
        val targetAnnouncement: Announcement = announcementQueryUseCase.getAnnouncementById(announcementId)
        eventPublisher.publishEvent(
            AnnouncementRemindEvent.of(
                announcement = targetAnnouncement,
                cohortId = cohortQueryUseCase.getLatestCohortId(),
            ),
        )
    }
}
