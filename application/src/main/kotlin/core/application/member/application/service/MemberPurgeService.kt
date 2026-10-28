package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.common.exception.GlobalExceptionCode
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberSharedRecordsException
import core.application.member.application.service.access.MemberAccessService
import core.domain.authorization.vo.RoleType
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

/** MASTER 전용. 회원과 연관 데이터를 전부 영구 삭제한다. 되돌릴 수 없다. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
class MemberPurgeService(
    private val members: MemberPersistencePort,
    private val memberAccessService: MemberAccessService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @TrackMemberBadges
    fun purge(
        memberId: Long,
        actorMemberId: Long,
    ) {
        if (memberId <= 0) {
            throw BusinessException(GlobalExceptionCode.INVALID_INPUT)
        }
        if (memberAccessService.getRoleType(MemberId(actorMemberId)) != RoleType.Master) {
            throw BusinessException(MemberExceptionCode.MEMBER_PURGE_MASTER_ONLY)
        }
        if (memberId == actorMemberId) {
            throw BusinessException(MemberExceptionCode.MEMBER_SELF_DELETION_NOT_ALLOWED)
        }
        // 소프트 삭제·탈퇴한 회원도 대상이다.
        if (members.lockApprovalTargets(listOf(memberId)).isEmpty()) {
            throw MemberNotFoundException()
        }
        val blockedMemberIds = members.findSharedRecordOwnerIds(listOf(memberId))
        if (blockedMemberIds.isNotEmpty()) {
            throw MemberSharedRecordsException(blockedMemberIds)
        }
        val imageObjectKeys = members.hardDeleteById(MemberId(memberId))
        eventPublisher.publishEvent(MemberPurgedEvent(memberId, imageObjectKeys))
    }
}

data class MemberPurgedEvent(
    val memberId: Long,
    val imageObjectKeys: List<String>,
)
