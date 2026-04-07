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
import core.application.member.application.service.authority.MemberAuthorityService
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
    val memberAuthorityService: MemberAuthorityService,
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

        val readMemberIds: List<MemberId> =
            announcementReads.filter { it.isRead() }.map { it.memberId }
        val readMembers: List<Member> = memberQueryUseCase.getMembersByIds(readMemberIds)
        val readMemberItems: List<AnnouncementViewMemberListItemResponse> =
            readMembers.map { readMember ->
                val teamNumber: TeamNumber = memberQueryUseCase.getMemberTeamNumber(readMember.id!!)
                val isAdmin: Boolean = memberAuthorityService.isAdmin(readMember.id!!)
                AnnouncementViewMemberListItemResponse.of(readMember, teamNumber, isAdmin)
            }

        val unreadMemberIds: List<MemberId> =
            announcementReads.filter { !it.isRead() }.map { it.memberId }
        val unreadMembers: List<Member> = memberQueryUseCase.getMembersByIds(unreadMemberIds)
        val unreadMemberItems: List<AnnouncementViewMemberListItemResponse> =
            unreadMembers.map { unreadMember ->
                val teamNumber: TeamNumber = memberQueryUseCase.getMemberTeamNumber(unreadMember.id!!)
                val isAdmin: Boolean = memberAuthorityService.isAdmin(unreadMember.id!!)
                AnnouncementViewMemberListItemResponse.of(unreadMember, teamNumber, isAdmin)
            }

        return AnnouncementViewMemberListResponse.of(
            readMembers = readMemberItems,
            unreadMembers = unreadMemberItems,
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

        val assignmentStatusMemberListItemResponses: List<AssignmentStatusMemberListItemResponse> =
            assignmentSubmissions.map { assignmentSubmission ->
                val member: Member = memberQueryUseCase.getMemberById(assignmentSubmission.memberId)
                val teamNumber: TeamNumber = memberQueryUseCase.getMemberTeamNumber(member.id!!)
                val isAdmin: Boolean = memberAuthorityService.isAdmin(member.id!!)
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
