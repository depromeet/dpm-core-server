package core.application.member.application.service

import core.application.attendance.application.service.AttendanceCommandService
import core.domain.afterParty.port.inbound.AfterPartyCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementCommandUseCase
import core.domain.member.event.MemberActivatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class MemberActivatedEventListener(
    val attendanceCommandService: AttendanceCommandService,
    val announcementCommandUseCase: AnnouncementCommandUseCase,
    val afterPartyCommandUseCase: AfterPartyCommandUseCase,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleMemberActivatedEvent(memberActivatedEvent: MemberActivatedEvent) {
        attendanceCommandService.initializeForNewCohortMember(
            memberId = memberActivatedEvent.memberId,
            cohortId = memberActivatedEvent.cohortId,
        )

        announcementCommandUseCase.initializeForNewCohortMember(
            memberId = memberActivatedEvent.memberId,
            cohortId = memberActivatedEvent.cohortId,
        )

        afterPartyCommandUseCase.initializeForNewCohortMember(
            memberId = memberActivatedEvent.memberId,
            cohortId = memberActivatedEvent.cohortId,
        )
    }
}
