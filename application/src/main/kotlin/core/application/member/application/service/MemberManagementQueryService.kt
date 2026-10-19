package core.application.member.application.service

import core.application.member.presentation.request.MemberManagementRequest
import core.application.member.presentation.request.MemberManagementRequest.ActivityStatus
import core.application.member.presentation.request.MemberManagementRequest.ApprovalStatus
import core.application.member.presentation.response.MemberManagementResponse
import core.application.member.presentation.response.MemberManagementResponse.MemberSummary
import core.application.member.presentation.response.MemberManagementResponse.Summary
import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.authorization.vo.RoleType
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.enums.MemberStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberManagementQueryService(
    private val targets: MemberManagementTargetQueryService,
    private val cohorts: CohortQueryUseCase,
) {
    @Transactional(readOnly = true)
    fun getOverview(request: MemberManagementRequest): MemberManagementResponse {
        val cohortId = cohorts.getLatestCohortId().value
        val allMembers = targets.findAll(cohortId)
        val search = request.search?.trim().orEmpty()
        val filtered =
            allMembers.asSequence()
                .filter { (it.status == MemberStatus.PENDING) == (request.approvalStatus == ApprovalStatus.PENDING) }
                .filter {
                    search.isEmpty() ||
                        it.name.contains(search, ignoreCase = true) ||
                        it.email?.contains(search, ignoreCase = true) == true
                }
                .filter { request.parts.isNullOrEmpty() || it.part in request.parts }
                .filter { request.teamNumbers.isNullOrEmpty() || it.teamNumber in request.teamNumbers }
                .filter { member ->
                    request.activityStatuses.isNullOrEmpty() ||
                        request.activityStatuses.any { activity ->
                            when (activity) {
                                ActivityStatus.NORMAL ->
                                    member.status == MemberStatus.ACTIVE && member.graduationStatus !in RISK_STATUSES
                                ActivityStatus.AT_RISK ->
                                    member.status == MemberStatus.ACTIVE && member.graduationStatus in RISK_STATUSES
                                ActivityStatus.INACTIVE -> member.status == MemberStatus.INACTIVE
                                null -> false
                            }
                        }
                }
                .filter { !request.excludeStaff || it.memberType !in STAFF_TYPES }
                .filter { !request.missingInformationOnly || it.missingInformation }
                .filter {
                    request.graduationStatuses.isNullOrEmpty() ||
                        (it.graduationStatus != null && it.graduationStatus in request.graduationStatuses)
                }
                .sortedWith(compareBy<MemberSummary> { it.name }.thenBy { it.memberId })
                .toList()
        // 수료 판정/필터를 끝낸 뒤 페이지를 나눈다. 큰 page 값도 정수 오버플로 없이 빈 목록이 된다.
        val offset = ((request.page.toLong() - 1) * request.size).coerceAtMost(filtered.size.toLong()).toInt()
        val approvedMembers =
            allMembers.filter {
                it.cohortId == cohortId && (it.status == MemberStatus.ACTIVE || it.status == MemberStatus.INACTIVE)
            }
        val deeper = approvedMembers.count { it.memberType == RoleType.Deeper.code }
        val organizer = approvedMembers.count { it.memberType == RoleType.Organizer.code }
        val core = approvedMembers.count { it.memberType == RoleType.Core.code }
        return MemberManagementResponse(
            cohortId = cohortId,
            summary =
                Summary(
                    totalMemberCount = approvedMembers.size,
                    deeperCount = deeper,
                    organizerCount = organizer,
                    coreCount = core,
                    pendingCount = allMembers.count { it.status == MemberStatus.PENDING },
                    missingInformationCount = allMembers.count { it.missingInformation },
                    graduationRiskCount = allMembers.count { it.graduationStatus in RISK_STATUSES },
                ),
            totalElements = filtered.size,
            page = request.page,
            size = request.size,
            lastUpdatedAt = allMembers.mapNotNull { it.updatedAt }.maxOrNull(),
            members = filtered.drop(offset).take(request.size),
        )
    }

    private companion object {
        val STAFF_TYPES = setOf(RoleType.Core.code, RoleType.Organizer.code)
        val RISK_STATUSES = setOf(AttendanceGraduationStatus.AT_RISK, AttendanceGraduationStatus.IMPOSSIBLE)
    }
}
