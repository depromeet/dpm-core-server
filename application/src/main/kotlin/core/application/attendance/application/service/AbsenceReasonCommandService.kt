package core.application.attendance.application.service

import core.application.session.application.service.SessionQueryService
import core.domain.absencereason.aggregate.AbsenceReason
import core.domain.absencereason.port.inbound.command.AbsenceReportCreateCommand
import core.domain.absencereason.port.outbound.AbsenceReasonPersistencePort
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.notification.event.AbsenceReasonSubmittedEvent
import core.domain.session.aggregate.Session
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AbsenceReasonCommandService(
    private val absenceReasonPersistencePort: AbsenceReasonPersistencePort,
    private val sessionQueryService: SessionQueryService,
    private val memberQueryUseCase: MemberQueryUseCase,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * 디퍼의 결석 사유서를 저장하고, 트랜잭션 커밋 후 운영진에게 알림을 발송하도록 이벤트를 발행한다.
     *
     * 이미 제출한 사유서가 있다면 내용을 갱신하고 검토 상태를 다시 PENDING 으로 되돌린다(재제출).
     */
    fun submitAbsenceReason(command: AbsenceReportCreateCommand) {
        val session: Session = sessionQueryService.getSessionById(command.sessionId)

        val absenceReason: AbsenceReason = absenceReasonPersistencePort
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
}
