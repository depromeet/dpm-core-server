package core.application.announcement.application.service

import core.application.announcement.application.exception.AnnouncementNotFoundException
import core.application.announcement.presentation.response.AnnouncementDetailResponse
import core.application.announcement.presentation.response.AnnouncementListResponse
import core.application.announcement.presentation.response.AnnouncementViewMemberListItemResponse
import core.application.announcement.presentation.response.AnnouncementViewMemberListResponse
import core.application.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.port.inbound.AnnouncementQueryUseCase
import core.domain.announcement.port.inbound.AnnouncementReadQueryUseCase
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.domain.announcement.port.outbound.query.AnnouncementListItemQueryModel
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AnnouncementQueryService(
    val announcementPersistencePort: AnnouncementPersistencePort,
    val announcementReadQueryUseCase: AnnouncementReadQueryUseCase,
    val memberQueryUseCase: MemberQueryUseCase,
) : AnnouncementQueryUseCase {
    fun getAllAnnouncements(): AnnouncementListResponse {
        val announcementListItemQueryModels: List<AnnouncementListItemQueryModel> =
            announcementPersistencePort.findAnnouncementListItems()

        return AnnouncementListResponse.from(announcementListItemQueryModels)
    }

    override fun getAnnouncementById(announcementId: AnnouncementId): Announcement =
        announcementPersistencePort.findAnnouncementById(announcementId) ?: throw AnnouncementNotFoundException()

    fun getAnnouncementDetail(announcementId: AnnouncementId): AnnouncementDetailResponse {
        val announcement: Announcement = getAnnouncementById(announcementId)
        val announcementReadCount: Int =
            announcementReadQueryUseCase.countByAnnouncementId(announcementId)

        return AnnouncementDetailResponse.of(
            title = announcement.title,
            content = announcement.content,
            createdAt = instantToLocalDateTime(announcement.createdAt!!),
            markAsReadCount = announcementReadCount,
        )
    }

    fun getAnnouncementReadMemberList(announcementId: AnnouncementId): AnnouncementViewMemberListResponse {
        val announcementReads: List<AnnouncementRead> = announcementReadQueryUseCase.getByAnnouncementId(announcementId)

        val readMemberIds: List<MemberId> =
            announcementReads.filter { it.isRead() }.map { it.memberId }
        val readMembers: List<Member> = memberQueryUseCase.getMembersByIds(readMemberIds)
        val readMemberItems: List<AnnouncementViewMemberListItemResponse> =
            readMembers.map { readMember ->
                val teamId: TeamId = memberQueryUseCase.getMemberTeamId(readMember.id!!)
                AnnouncementViewMemberListItemResponse.of(readMember, teamId)
            }

        val unreadMemberIds: List<MemberId> =
            announcementReads.filter { !it.isRead() }.map { it.memberId }
        val unreadMembers: List<Member> = memberQueryUseCase.getMembersByIds(unreadMemberIds)
        val unreadMemberItems: List<AnnouncementViewMemberListItemResponse> =
            unreadMembers.map { unreadMember ->
                val teamId: TeamId = memberQueryUseCase.getMemberTeamId(unreadMember.id!!)
                AnnouncementViewMemberListItemResponse.of(unreadMember, teamId)
            }

        return AnnouncementViewMemberListResponse.of(
            readMembers = readMemberItems,
            unreadMembers = unreadMemberItems,
        )
    }
}
