package core.application.announcement.application.service

import core.application.announcement.application.exception.AssignmentSubmitTypeNotNullException
import core.application.cohort.application.properties.CohortProperties
import core.application.member.application.service.MemberQueryService
import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.aggregate.AnnouncementAssignment
import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitStatus
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.port.inbound.AnnouncementCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementQueryUseCase
import core.domain.announcement.port.inbound.AnnouncementReadCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementReadQueryUseCase
import core.domain.announcement.port.inbound.AssignmentQueryUseCase
import core.domain.announcement.port.inbound.AssignmentSubmissionCommandUseCase
import core.domain.announcement.port.inbound.AssignmentSubmissionQueryUseCase
import core.domain.announcement.port.outbound.AnnouncementAssignmentPersistencePort
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.domain.announcement.port.outbound.AssignmentPersistencePort
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class AnnouncementCommandService(
    val announcementPersistencePort: AnnouncementPersistencePort,
    val announcementAssignmentPersistencePort: AnnouncementAssignmentPersistencePort,
    val assignmentPersistencePort: AssignmentPersistencePort,
    val announcementReadCommandUseCase: AnnouncementReadCommandUseCase,
    val announcementQueryUseCase: AnnouncementQueryUseCase,
    val announcementReadQueryUseCase: AnnouncementReadQueryUseCase,
    val assignmentQueryUseCase: AssignmentQueryUseCase,
    val assignmentSubmissionQueryUseCase: AssignmentSubmissionQueryUseCase,
    val assignmentSubmissionCommandUseCase: AssignmentSubmissionCommandUseCase,
    val memberQueryService: MemberQueryService,
    val cohortProperties: CohortProperties,
) : AnnouncementCommandUseCase {
    override fun create(
        authorId: MemberId,
        announcementType: AnnouncementType,
        submitType: SubmitType?,
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
                announcementAssignmentPersistencePort.save(announcementAssignment)
//                TODO : Assignment_submission에 디퍼들 초대하는 로직 추가
            }
        }

        val memberIds: List<MemberId> = memberQueryService.getMembersByCohort(cohortProperties.value)
        announcementReadCommandUseCase.initializeForMembers(
            announcementId = savedAnnouncement.id!!,
            memberIds = memberIds,
        )
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
    ) {
        val retrievedAssignment: Assignment =
            assignmentQueryUseCase.getAssignmentByAnnouncementId(announcementId)

        memberIds.forEach { memberId ->
            val memberAssignmentSubmission: AssignmentSubmission =
                assignmentSubmissionQueryUseCase.getByAssignmentIdAndMemberId(
                    assignmentId = retrievedAssignment.id!!,
                    memberId = memberId,
                )
            val updatedAssignmentSubmission: AssignmentSubmission =
                memberAssignmentSubmission.updateSubmitStatus(submitStatus)
            assignmentSubmissionCommandUseCase.updateAssignmentSubmission(updatedAssignmentSubmission)
        }
    }
}
