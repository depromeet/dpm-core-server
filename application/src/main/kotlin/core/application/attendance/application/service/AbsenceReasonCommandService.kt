package core.application.attendance.application.service

import core.application.attendance.application.exception.AbsenceReasonNotFoundException
import core.application.attendance.application.exception.AbsenceReasonRequiredException
import core.application.session.application.service.SessionQueryService
import core.domain.absencereason.aggregate.AbsenceReason
import core.domain.absencereason.port.inbound.command.AbsenceReasonReviewCommand
import core.domain.absencereason.port.inbound.command.AbsenceReportCreateCommand
import core.domain.absencereason.port.inbound.command.AbsenceReportUpdateCommand
import core.domain.absencereason.port.outbound.AbsenceReasonPersistencePort
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.notification.event.AbsenceReasonSubmittedEvent
import core.domain.session.aggregate.Session
import core.domain.session.vo.SessionId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AbsenceReasonCommandService(
    private val absenceReasonPersistencePort: AbsenceReasonPersistencePort,
    private val sessionQueryService: SessionQueryService,
    private val memberQueryUseCase: MemberQueryUseCase,
    private val attendanceCommandService: AttendanceCommandService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * 디퍼의 결석 사유서를 저장하고, 트랜잭션 커밋 후 운영진에게 알림을 발송하도록 이벤트를 발행한다.
     *
     * 이미 제출한 사유서가 있다면 내용을 갱신하고 검토 상태를 다시 PENDING 으로 되돌린다(재제출).
     */
    fun submitAbsenceReason(command: AbsenceReportCreateCommand) {
        if (command.contents.isBlank()) {
            throw AbsenceReasonRequiredException()
        }

        val session: Session = sessionQueryService.getSessionById(command.sessionId)

        val absenceReason: AbsenceReason =
            absenceReasonPersistencePort
                .findBySessionIdAndMemberId(command.sessionId.value, command.memberId.value)
                ?.apply { resubmit(command.contents) }
                ?: AbsenceReason.create(command)

        absenceReasonPersistencePort.save(absenceReason)

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

    /**
     * 본인이 제출한 결석 사유서의 내용을 수정한다.
     *
     * 수정 시 검토 상태는 다시 PENDING 으로 되돌아간다.
     * 제출한 사유서가 없으면 [AbsenceReasonNotFoundException] 을 던진다.
     */
    fun updateAbsenceReason(command: AbsenceReportUpdateCommand) {
        if (command.contents.isBlank()) {
            throw AbsenceReasonRequiredException()
        }

        val absenceReason =
            absenceReasonPersistencePort
                .findBySessionIdAndMemberId(command.sessionId.value, command.memberId.value)
                ?: throw AbsenceReasonNotFoundException()

        absenceReason.resubmit(command.contents)
        absenceReasonPersistencePort.save(absenceReason)
    }

    /**
     * 본인이 제출한 결석 사유서를 삭제한다.
     *
     * 제출한 사유서가 없으면 [AbsenceReasonNotFoundException] 을 던진다.
     */
    fun deleteAbsenceReason(
        sessionId: SessionId,
        memberId: MemberId,
    ) {
        val absenceReason =
            absenceReasonPersistencePort
                .findBySessionIdAndMemberId(sessionId.value, memberId.value)
                ?: throw AbsenceReasonNotFoundException()

        absenceReasonPersistencePort.delete(absenceReason)
    }

    /**
     * 운영진이 결석 사유서를 검토(승인/반려)한다.
     *
     * 승인 시 해당 멤버의 출석 상태를 인정결석([AttendanceStatus.EXCUSED_ABSENT])으로 변경한다.
     * 반려 시 출석 상태는 결석 그대로 유지된다.
     * 제출된 사유서가 없으면 [AbsenceReasonNotFoundException] 을 던진다.
     */
    fun reviewAbsenceReason(command: AbsenceReasonReviewCommand) {
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
}
