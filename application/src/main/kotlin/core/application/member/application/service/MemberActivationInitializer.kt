package core.application.member.application.service

import core.application.attendance.application.service.AttendanceCommandService
import core.domain.afterParty.port.inbound.AfterPartyCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementCommandUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.MANDATORY)
class MemberActivationInitializer(
    private val attendanceCommandService: AttendanceCommandService,
    private val announcementCommandUseCase: AnnouncementCommandUseCase,
    private val afterPartyCommandUseCase: AfterPartyCommandUseCase,
) {
    fun initialize(
        memberId: MemberId,
        cohortId: CohortId,
    ) {
        attendanceCommandService.initializeForNewCohortMember(memberId, cohortId)
        announcementCommandUseCase.initializeForNewCohortMember(memberId, cohortId)
        afterPartyCommandUseCase.initializeForNewCohortMember(memberId, cohortId)
    }
}
