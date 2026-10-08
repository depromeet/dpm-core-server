package core.application.member.application.service

import core.application.attendance.application.service.AttendanceGraduationEvaluator
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.application.member.presentation.response.MemberManagementResponse.MemberSummary
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.authorization.vo.RoleType
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import org.springframework.stereotype.Service

@Service
class MemberManagementTargetQueryService(
    private val members: MemberPersistencePort,
    private val roles: MemberRolePersistencePort,
    private val attendances: AttendancePersistencePort,
    private val roleResolver: CurrentCohortRoleResolver,
    private val graduationEvaluator: AttendanceGraduationEvaluator,
) {
    fun findAll(cohortId: Long): List<MemberSummary> {
        val source = members.findManagementMembers(cohortId)
        val memberIds = source.map { it.memberId }
        val assignments = roles.findActiveRoleAssignmentsByMemberIds(memberIds)
        val attendanceSummaries =
            attendances.findMemberAttendances(cohortId, emptyList()).associate { it.id to it.summary }
        val duplicateCounts =
            source.filter { it.name.isNotBlank() && it.part != null }
                .groupingBy { it.name.trim() to it.part }
                .eachCount()
        return source.map { member ->
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
                email = member.email,
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
                duplicateSuspected = duplicateCounts.getOrDefault(member.name.trim() to member.part, 0) > 1,
                updatedAt = member.updatedAt,
            )
        }
    }

    private companion object {
        const val UNASSIGNED = "UNASSIGNED"
        val DISPLAY_TYPES = setOf(RoleType.Deeper, RoleType.Organizer, RoleType.Core)
    }
}
