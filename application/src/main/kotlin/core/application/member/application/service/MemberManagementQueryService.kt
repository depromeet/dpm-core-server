package core.application.member.application.service

import core.application.attendance.application.service.AttendanceGraduationEvaluator
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.application.member.presentation.request.MemberManagementRequest
import core.application.member.presentation.request.MemberManagementRequest.ApprovalStatus
import core.application.member.presentation.response.MemberManagementResponse
import core.application.member.presentation.response.MemberManagementResponse.MemberSummary
import core.application.member.presentation.response.MemberManagementResponse.Summary
import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.authorization.vo.RoleType
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberManagementQueryService(
    private val members: MemberPersistencePort,
    private val roles: MemberRolePersistencePort,
    private val attendances: AttendancePersistencePort,
    private val cohorts: CohortQueryUseCase,
    private val roleResolver: CurrentCohortRoleResolver,
    private val graduationEvaluator: AttendanceGraduationEvaluator,
) {
    @Transactional(readOnly = true)
    fun getOverview(request: MemberManagementRequest): MemberManagementResponse {
        val cohortId = cohorts.getLatestCohortId().value
        val source = members.findManagementMembers(cohortId)
        val memberIds = source.map { it.memberId }
        val assignments = roles.findActiveRoleAssignmentsByMemberIds(memberIds)
        val attendanceSummaries =
            attendances.findMemberAttendances(cohortId, emptyList()).associate { it.id to it.summary }
        val allMembers =
            source.map { member ->
                val role =
                    roleResolver.findPrimaryRoleType(
                        assignments =
                            assignments[member.memberId].orEmpty().filter { it.roleName != RoleType.Master.code },
                        context = CurrentCohortRoleResolver.CohortRoleContext(cohortId, setOfNotNull(member.cohortId)),
                    )
                val type = role.code.takeIf { role in DISPLAY_TYPES } ?: UNASSIGNED
                val approved = member.status != MemberStatus.PENDING
                MemberSummary(
                    memberId = member.memberId,
                    cohortId = member.cohortId,
                    name = member.name,
                    signupEmail = member.signupEmail,
                    part = member.part?.name ?: UNASSIGNED,
                    memberType = type,
                    teamNumber = member.teamNumber,
                    status = member.status,
                    missingInformation =
                        approved && (member.part == null || member.teamNumber == 0 || type == UNASSIGNED),
                    graduationStatus =
                        attendanceSummaries[member.memberId]?.takeIf { approved && member.cohortId == cohortId }?.let(
                            graduationEvaluator::evaluate,
                        ),
                    duplicateSuspected = null,
                    updatedAt = member.updatedAt,
                )
            }
        val search = request.search?.trim().orEmpty()
        val filtered =
            allMembers.asSequence()
                .filter { (it.status == MemberStatus.PENDING) == (request.approvalStatus == ApprovalStatus.PENDING) }
                .filter {
                    search.isEmpty() ||
                        it.name.contains(search, ignoreCase = true) ||
                        it.signupEmail.contains(search, ignoreCase = true)
                }
                .filter { request.part == null || it.part == request.part }
                .filter { request.teamNumber == null || it.teamNumber == request.teamNumber }
                .filter { request.status == null || it.status.name == request.status }
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
        val deeper = allMembers.count { it.memberType == RoleType.Deeper.code }
        val organizer = allMembers.count { it.memberType == RoleType.Organizer.code }
        val core = allMembers.count { it.memberType == RoleType.Core.code }
        return MemberManagementResponse(
            cohortId = cohortId,
            summary =
                Summary(
                    totalMemberCount = deeper + organizer + core,
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
        const val UNASSIGNED = "UNASSIGNED"
        val DISPLAY_TYPES = setOf(RoleType.Deeper, RoleType.Organizer, RoleType.Core)
        val STAFF_TYPES = setOf(RoleType.Core.code, RoleType.Organizer.code)
        val RISK_STATUSES = setOf(AttendanceGraduationStatus.AT_RISK, AttendanceGraduationStatus.IMPOSSIBLE)
    }
}
