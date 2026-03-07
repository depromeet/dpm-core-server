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
        // TODO : 해당 기수의 출석부에 추가
        attendanceCommandService.initializeForNewCohortMember(
            memberId = memberActivatedEvent.memberId,
            cohortId = memberActivatedEvent.cohortId,
        )

        // TODO : 해당 기수의 공지 읽음 이력, 과제 제출 현황에 추가. 지금은 공지/과제가 기수 별로 나눠져 있지 않은 상태라서, 공지 읽음 이력도 기수 별로 나눠야 할듯
        announcementCommandUseCase.initializeForNewCohortMember(
            memberId = memberActivatedEvent.memberId,
            cohortId = memberActivatedEvent.cohortId,
        )

        // TODO : 해당 기수의 회식 참석 현황에 추가
        afterPartyCommandUseCase.initializeForNewCohortMember(
            memberId = memberActivatedEvent.memberId,
            cohortId = memberActivatedEvent.cohortId,
        )
    }
}
