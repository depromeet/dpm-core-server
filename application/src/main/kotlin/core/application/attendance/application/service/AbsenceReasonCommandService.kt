package core.application.attendance.application.service

import core.application.attendance.application.exception.AbsenceReasonImageAlreadyAttachedException
import core.application.attendance.application.exception.AbsenceReasonNotFoundException
import core.application.attendance.application.exception.AbsenceReasonRequiredException
import core.application.attendance.application.exception.AbsenceReasonTooLongException
import core.application.attendance.application.exception.AttendanceExceptionCode
import core.application.attendance.application.exception.InvalidAbsenceReasonImageException
import core.application.member.application.service.TrackMemberBadges
import core.application.session.application.exception.SessionNotFoundException
import core.domain.absencereason.aggregate.AbsenceReason
import core.domain.absencereason.port.inbound.command.AbsenceReasonReviewCommand
import core.domain.absencereason.port.inbound.command.AbsenceReportCreateCommand
import core.domain.absencereason.port.inbound.command.AbsenceReportUpdateCommand
import core.domain.absencereason.port.outbound.AbsenceReasonImageConflictException
import core.domain.absencereason.port.outbound.AbsenceReasonImagePersistencePort
import core.domain.absencereason.port.outbound.AbsenceReasonPersistencePort
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.vo.ImageId
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.notification.event.AbsenceReasonSubmittedEvent
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/** 모든 변경은 세션 행 쓰기 잠금을 먼저 잡는다(잠금 순서 세션 -> 출석 -> 사유서). */
@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
class AbsenceReasonCommandService(
    private val absenceReasonPersistencePort: AbsenceReasonPersistencePort,
    private val absenceReasonImagePersistencePort: AbsenceReasonImagePersistencePort,
    private val imagePersistencePort: ImagePersistencePort,
    private val sessionPersistencePort: SessionPersistencePort,
    private val memberQueryUseCase: MemberQueryUseCase,
    private val attendanceCommandService: AttendanceCommandService,
    private val attendancePersistencePort: AttendancePersistencePort,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    /**
     * 사유서를 저장(이미 있으면 재제출)하면서 바로 승인하고 출석을 인정결석으로 바꾼다.
     * 커밋 후 운영진 알림 이벤트를 발행한다.
     */
    @TrackMemberBadges
    fun submitAbsenceReason(command: AbsenceReportCreateCommand) {
        validateRequest(command.contents, command.imageIds)

        val session: Session = lockSession(command.sessionId)

        val absenceReason: AbsenceReason =
            absenceReasonPersistencePort
                .findBySessionIdAndMemberId(command.sessionId.value, command.memberId.value)
                ?.apply { resubmit(command.contents) }
                ?: AbsenceReason.create(command)

        saveWithImages(absenceReason, command.memberId, command.imageIds)
        excuseAbsence(command.sessionId, command.memberId)

        val submitter = memberQueryUseCase.getMemberById(command.memberId)

        eventPublisher.publishEvent(
            AbsenceReasonSubmittedEvent(
                cohortId = session.cohortId,
                sessionId = command.sessionId,
                memberId = command.memberId,
                submitterName = submitter.name,
                week = session.week,
            ),
        )
    }

    /** 본인 사유서를 수정하고 다시 승인한다. 반려됐던 사유서도 인정결석으로 바뀐다. 없으면 [AbsenceReasonNotFoundException]. */
    @TrackMemberBadges
    fun updateAbsenceReason(command: AbsenceReportUpdateCommand) {
        validateRequest(command.contents, command.imageIds)

        lockSession(command.sessionId)

        val absenceReason =
            absenceReasonPersistencePort
                .findBySessionIdAndMemberId(command.sessionId.value, command.memberId.value)
                ?: throw AbsenceReasonNotFoundException()

        absenceReason.resubmit(command.contents)
        saveWithImages(absenceReason, command.memberId, command.imageIds)
        excuseAbsence(command.sessionId, command.memberId)
    }

    /**
     * 첨부 링크와 함께 삭제하고 인정결석을 사유서가 없었을 때의 상태로 되돌린다(마감 전 PENDING, 마감 후 자동 결석).
     * 삭제된 세션의 사유서도 지울 수 있도록 세션이 없으면 잠금과 출석 변경 없이 진행한다.
     */
    @TrackMemberBadges
    fun deleteAbsenceReason(
        sessionId: SessionId,
        memberId: MemberId,
    ) {
        val session = sessionPersistencePort.findSessionByIdForUpdate(sessionId.value)

        val absenceReason =
            absenceReasonPersistencePort
                .findBySessionIdAndMemberId(sessionId.value, memberId.value)
                ?: throw AbsenceReasonNotFoundException()

        absenceReason.id?.let { absenceReasonImagePersistencePort.deleteAll(it.value) }
        absenceReasonPersistencePort.delete(absenceReason)

        if (session != null) {
            val now = clock.instant()
            attendancePersistencePort.releaseExcusedAbsence(
                sessionId = sessionId.value,
                memberId = memberId.value,
                closedAt = now.takeIf { session.isAttendanceClosedAt(it) },
            )
        }
    }

    /** 승인 시 출석을 인정결석([AttendanceStatus.EXCUSED_ABSENT])으로 바꾸고, 반려 시 출석은 그대로 둔다. */
    @TrackMemberBadges
    fun reviewAbsenceReason(command: AbsenceReasonReviewCommand) {
        lockSession(command.sessionId)

        val absenceReason =
            absenceReasonPersistencePort
                .findBySessionIdAndMemberId(command.sessionId.value, command.memberId.value)
                ?: throw AbsenceReasonNotFoundException()

        if (command.approved) {
            absenceReason.approve()
            attendanceCommandService.updateAttendanceStatus(
                AttendanceStatusUpdateCommand(
                    sessionId = command.sessionId,
                    memberId = command.memberId,
                    attendanceStatus = AttendanceStatus.EXCUSED_ABSENT,
                ),
            )
        } else {
            absenceReason.reject()
        }

        absenceReasonPersistencePort.save(absenceReason)
    }

    /** 출석·지각은 그대로 두고, 출석 행이 없어도 사유서 제출은 막지 않는다. */
    private fun excuseAbsence(
        sessionId: SessionId,
        memberId: MemberId,
    ) {
        attendancePersistencePort.excuseAbsence(sessionId.value, memberId.value, clock.instant())
    }

    private fun lockSession(sessionId: SessionId): Session =
        sessionPersistencePort.findSessionByIdForUpdate(sessionId.value) ?: throw SessionNotFoundException()

    private fun validateRequest(
        contents: String,
        imageIds: List<ImageId>?,
    ) {
        if (contents.isBlank()) throw AbsenceReasonRequiredException()
        // VARCHAR(50) 은 문자 수 기준이라 UTF-16 길이가 아니라 코드 포인트로 센다.
        if (contents.codePointCount(0, contents.length) > MAX_CONTENTS_LENGTH) throw AbsenceReasonTooLongException()
        if (imageIds == null) return
        if (imageIds.any { it.value <= 0 }) throw InvalidAbsenceReasonImageException()
        if (imageIds.toSet().size != imageIds.size) {
            throw InvalidAbsenceReasonImageException(AttendanceExceptionCode.DUPLICATE_ABSENCE_REASON_IMAGE)
        }
    }

    /**
     * [imageIds] 가 null 이면 첨부는 그대로 두고 사유서만 저장한다.
     * 일반 중복 첨부는 DB 오류 로그를 남기지 않도록 미리 거른다. 동시 요청은 UNIQUE 제약으로 막는다.
     */
    private fun saveWithImages(
        absenceReason: AbsenceReason,
        memberId: MemberId,
        imageIds: List<ImageId>?,
    ) {
        if (imageIds == null) {
            absenceReasonPersistencePort.save(absenceReason)
            return
        }

        val owned = imagePersistencePort.findAllByIds(imageIds).count { it.isOwnedBy(memberId) }
        if (owned != imageIds.size) throw InvalidAbsenceReasonImageException()

        val absenceReasonId = requireNotNull(absenceReasonPersistencePort.save(absenceReason).id).value
        val attachedElsewhere =
            absenceReasonImagePersistencePort
                .findAbsenceReasonIdsByImageIds(imageIds)
                .any { (_, linkedReasonId) -> linkedReasonId != absenceReasonId }
        if (attachedElsewhere) throw AbsenceReasonImageAlreadyAttachedException()

        try {
            absenceReasonImagePersistencePort.replaceImages(absenceReasonId, imageIds)
        } catch (_: AbsenceReasonImageConflictException) {
            throw AbsenceReasonImageAlreadyAttachedException()
        }
    }

    companion object {
        /** absence_reasons.contents 컬럼 길이 */
        const val MAX_CONTENTS_LENGTH = 50
    }
}
