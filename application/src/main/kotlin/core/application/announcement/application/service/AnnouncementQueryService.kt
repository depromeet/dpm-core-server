package core.application.announcement.application.service

import core.application.announcement.application.exception.AnnouncementNotFoundException
import core.application.announcement.application.exception.AssignmentNotFoundException
import core.application.announcement.application.exception.NotAnAssignmentException
import core.application.announcement.presentation.response.AnnouncementDetailAssignmentResponse
import core.application.announcement.presentation.response.AnnouncementDetailResponse
import core.application.announcement.presentation.response.AnnouncementListResponse
import core.application.announcement.presentation.response.AnnouncementViewMemberListItemResponse
import core.application.announcement.presentation.response.AnnouncementViewMemberListResponse
import core.application.announcement.presentation.response.AssignmentStatusMemberListItemResponse
import core.application.announcement.presentation.response.AssignmentStatusMemberListResponse
import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.application.member.application.service.access.MemberAccessService
import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.port.inbound.AnnouncementQueryUseCase
import core.domain.announcement.port.inbound.AnnouncementReadCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementReadQueryUseCase
import core.domain.announcement.port.inbound.AssignmentQueryUseCase
import core.domain.announcement.port.inbound.AssignmentSubmissionQueryUseCase
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.domain.announcement.port.outbound.query.AnnouncementListItemQueryModel
import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AssignmentId
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AnnouncementQueryService(
    val announcementPersistencePort: AnnouncementPersistencePort,
    val announcementReadQueryUseCase: AnnouncementReadQueryUseCase,
    val announcementReadCommandUseCase: AnnouncementReadCommandUseCase,
    val assignmentSubmissionQueryUseCase: AssignmentSubmissionQueryUseCase,
    val assignmentQueryUseCase: AssignmentQueryUseCase,
    val memberQueryUseCase: MemberQueryUseCase,
    val memberAccessService: MemberAccessService,
    val cohortQueryUseCase: CohortQueryUseCase,
) : AnnouncementQueryUseCase {
    fun getAllAnnouncements(): AnnouncementListResponse {
        val announcementListItemQueryModels: List<AnnouncementListItemQueryModel> =
            announcementPersistencePort.findAnnouncementListItems().sortedByDescending { it.createdAt }

        return AnnouncementListResponse.from(announcementListItemQueryModels)
    }

    override fun getAnnouncementById(announcementId: AnnouncementId): Announcement =
        announcementPersistencePort.findAnnouncementById(announcementId) ?: throw AnnouncementNotFoundException()

    override fun findAnnouncementByAssignmentId(assignmentId: AssignmentId): Announcement? =
        announcementPersistencePort.findByAssignmentId(assignmentId)

    override fun getAll(): List<Announcement> = announcementPersistencePort.findAll()

    override fun findUnreadByAnnouncementId(announcementId: AnnouncementId): List<AnnouncementRead> =
        announcementReadQueryUseCase.findUnreadByAnnouncementId(
            announcementId,
        )

    override fun findUnsubmittedByAnnouncementIdAndSubmitStatus(
        announcementId: AnnouncementId,
    ): List<AssignmentSubmission> {
        val assignment: Assignment = assignmentQueryUseCase.getAssignmentByAnnouncementId(announcementId)
        return assignmentSubmissionQueryUseCase.findUnsubmittedByAssignmentIdAndSubmitStatus(
            assignment.id ?: throw AssignmentNotFoundException(),
        )
    }

    @Transactional(readOnly = false)
    fun getAnnouncementDetail(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): AnnouncementDetailResponse {
        val announcement: Announcement = getAnnouncementById(announcementId)
//        TODO : Get에서 POST 문제가 있다..!
        val announcementRead: AnnouncementRead =
            announcementReadQueryUseCase.findByAnnouncementIdAndMemberId(announcementId, memberId)
                ?: announcementReadCommandUseCase.create(announcementId, memberId)

        val announcementReadCount: Int =
            announcementReadQueryUseCase.readMemberCountByAnnouncementId(announcementId)
        when (announcement.announcementType) {
            AnnouncementType.GENERAL -> return AnnouncementDetailResponse.of(
                announcementId = announcementId,
                announcementType = announcement.announcementType,
                title = announcement.title,
                content = announcement.content,
                createdAt = instantToLocalDateTime(announcement.createdAt!!),
                isRead = announcementRead.isRead(),
                markAsReadCount = announcementReadCount,
            )

            AnnouncementType.ASSIGNMENT -> {
                val assignment: Assignment = assignmentQueryUseCase.getAssignmentByAnnouncementId(announcementId)

                return AnnouncementDetailResponse.of(
                    announcementId = announcementId,
                    announcementType = announcement.announcementType,
                    title = announcement.title,
                    content = announcement.content,
                    createdAt = instantToLocalDateTime(announcement.createdAt!!),
                    announcementDetailAssignmentResponse =
                        AnnouncementDetailAssignmentResponse.of(
                            submitType = assignment.submitType,
                            startAt = instantToLocalDateTime(assignment.startAt),
                            dueAt = instantToLocalDateTime(assignment.dueAt),
                            submitLink = assignment.submitLink,
                        ),
                    isRead = announcementRead.isRead(),
                    markAsReadCount = announcementReadCount,
                )
            }
        }
    }

    fun getAnnouncementReadMemberList(announcementId: AnnouncementId): AnnouncementViewMemberListResponse {
        val announcementReads: List<AnnouncementRead> = announcementReadQueryUseCase.getByAnnouncementId(announcementId)
        val announcementReadMemberIds: List<MemberId> = announcementReads.map { it.memberId }
        val currentCohort: String = cohortQueryUseCase.getLatestCohortValue()

        val retrievedMembers: List<Member> = memberQueryUseCase.getMembersByIds(announcementReadMemberIds)
        val retrievedMemberTeamNumberMap: Map<MemberId, TeamNumber> =
            memberQueryUseCase.getMemberTeamNumberByMemberIds(
                announcementReadMemberIds,
            )
        val retrievedMemberAdminMap: Map<MemberId, Boolean> =
            memberAccessService.getIsAdminByMemberIds(
                memberIds = announcementReadMemberIds,
                cohortValue = currentCohort,
            )

        val memberItems: List<AnnouncementViewMemberListItemResponse> =
            retrievedMembers.map { member ->
                val teamNumber: TeamNumber = retrievedMemberTeamNumberMap[member.id!!] ?: TeamNumber(0)
                val isAdmin: Boolean = retrievedMemberAdminMap[member.id!!] ?: false
                AnnouncementViewMemberListItemResponse.of(member, teamNumber, isAdmin)
            }.sortedWith(compareBy({ it.teamNumber.value }, { it.name }))
        val readMemberIds: Set<MemberId> =
            announcementReads.filter { it.isRead() }.map { it.memberId }.toSet()

        val unreadMemberIds: Set<MemberId> =
            announcementReads.filter { !it.isRead() }.map { it.memberId }.toSet()

        return AnnouncementViewMemberListResponse.of(
            readMembers = memberItems.filter { it.memberId in readMemberIds },
            unreadMembers = memberItems.filter { it.memberId in unreadMemberIds },
        )
    }

    fun getAssignmentStatusMemberList(announcementId: AnnouncementId): AssignmentStatusMemberListResponse {
        val announcement: Announcement = getAnnouncementById(announcementId)
        if (announcement.announcementType != AnnouncementType.ASSIGNMENT) throw NotAnAssignmentException()

        val assignment: Assignment =
            assignmentQueryUseCase.getAssignmentByAnnouncementId(
                announcement.id ?: throw AnnouncementNotFoundException(),
            )
        val assignmentSubmissions: List<AssignmentSubmission> =
            assignmentSubmissionQueryUseCase.getByAssignmentId(
                assignment.id ?: throw AssignmentNotFoundException(),
            )
        val assignmentSubmissionMemberIds: List<MemberId> = assignmentSubmissions.map { it.memberId }
        val assignmentSubmissionMembers: List<Member> =
            memberQueryUseCase.getMembersByIds(
                assignmentSubmissionMemberIds,
            )
        val assignmentSubmissionMemberTeamNumberMap: Map<MemberId, TeamNumber> =
            memberQueryUseCase.getMemberTeamNumberByMemberIds(
                assignmentSubmissionMemberIds,
            )
        val assignmentSubmissionMemberAdminMap: Map<MemberId, Boolean> =
            memberAccessService.getIsAdminByMemberIds(
                memberIds = assignmentSubmissionMemberIds,
                cohortValue = cohortQueryUseCase.getLatestCohortValue(),
            )

        val assignmentStatusMemberListItemResponses: List<AssignmentStatusMemberListItemResponse> =
            assignmentSubmissions.map { assignmentSubmission ->
                val member: Member =
                    assignmentSubmissionMembers.find { it.id == assignmentSubmission.memberId }
                        ?: throw AnnouncementNotFoundException()
                val teamNumber: TeamNumber = assignmentSubmissionMemberTeamNumberMap[member.id!!] ?: TeamNumber(0)
                val isAdmin: Boolean = assignmentSubmissionMemberAdminMap[member.id!!] ?: false
                AssignmentStatusMemberListItemResponse.of(
                    memberId = member.id!!,
                    name = member.name,
                    teamNumber = teamNumber,
                    isAdmin = isAdmin,
                    part = member.part,
                    submitStatus = assignmentSubmission.submitStatus,
                    score = assignmentSubmission.score,
                )
            }

        return AssignmentStatusMemberListResponse.from(assignmentStatusMemberListItemResponses)
    }
}
