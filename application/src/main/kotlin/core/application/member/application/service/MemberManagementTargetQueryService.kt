package core.application.member.application.service

import core.application.attendance.application.service.AttendanceGraduationEvaluator
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.application.member.presentation.response.MemberManagementResponse.MemberSummary
import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.authorization.vo.RoleType
import core.domain.member.constant.TestAccountEmails
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberActivityPersistencePort
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
    private val activities: MemberActivityPersistencePort,
) {
    /**
     * [cohortId] 기수의 관리 대상을 계산한다. 가입 대기자는 [activeCohortId] 기준으로 고정한다.
     * 활성 기수가 아닌 기수는 활동을 마친 기수라 수료 상태를 판정하지 않고 [AttendanceGraduationStatus.COMPLETED]로 둔다.
     */
    fun findAll(
        cohortId: Long,
        activeCohortId: Long = cohortId,
    ): List<MemberSummary> {
        val isActiveCohort = cohortId == activeCohortId
        val source =
            members.findManagementMembers(cohortId, pendingCohortId = activeCohortId)
                .filterNot { TestAccountEmails.matches(it.signupEmail) }
        val memberIds = source.map { it.memberId }
        val assignments = roles.findActiveRoleAssignmentsByMemberIds(memberIds)
        val activityByMember = activities.findAll(memberIds, cohortId)
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
                loginMethod = member.loginMethod,
                part = member.part?.name ?: UNASSIGNED,
                memberType = type,
                teamNumber = member.teamNumber,
                status = member.status,
                missingInformation =
                    approved && (member.part == null || member.teamNumber == 0 || type == UNASSIGNED),
                graduationStatus =
                    attendanceSummaries[member.memberId]?.takeIf { approved && member.cohortId == cohortId }?.let {
                        if (isActiveCohort) graduationEvaluator.evaluate(it) else AttendanceGraduationStatus.COMPLETED
                    },
                duplicateSuspected = duplicateCounts.getOrDefault(member.name.trim() to member.part, 0) > 1,
                updatedAt = member.updatedAt,
                activityStatus = activityByMember[member.memberId]?.status,
            )
        }
    }

    private companion object {
        const val UNASSIGNED = "UNASSIGNED"
        val DISPLAY_TYPES = setOf(RoleType.Deeper, RoleType.Organizer, RoleType.Core)
    }
}
