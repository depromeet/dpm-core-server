package core.application.member.application.service

import core.application.member.application.exception.InvalidMemberApprovalException
import core.application.member.application.exception.MemberApprovalTargetNotAllowedException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberNotFoundException
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.vo.RoleType
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.aggregate.MemberCohort
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberCohortPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.port.outbound.MemberTeamPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberApprovalService(
    private val members: MemberPersistencePort,
    private val memberCohorts: MemberCohortPersistencePort,
    private val teams: MemberTeamPersistencePort,
    private val roles: MemberRolePersistencePort,
    private val cohorts: CohortQueryUseCase,
    private val roleQueries: RoleQueryUseCase,
    private val initializer: MemberActivationInitializer,
) {
    fun approve(memberIds: List<Long?>) {
        if (memberIds.isEmpty() || memberIds.any { it == null || it <= 0 } ||
            memberIds.distinct().size != memberIds.size
        ) {
            throw InvalidMemberApprovalException()
        }
        val ids = memberIds.filterNotNull().sorted()
        val cohortId = cohorts.getActiveCohortId()
        val targets = members.lockApprovalTargets(ids)
        if (targets.map { it.memberId } != ids) throw MemberNotFoundException()
        if (targets.any { it.isDeleted }) throw MemberDeletedException()
        val hasInvalidTarget =
            targets.any { target ->
                when (target.status) {
                    MemberStatus.PENDING -> target.cohortIds.isNotEmpty() && cohortId.value !in target.cohortIds
                    MemberStatus.ACTIVE, MemberStatus.INACTIVE -> cohortId.value !in target.cohortIds
                    else -> true
                }
            }
        if (hasInvalidTarget) {
            throw MemberApprovalTargetNotAllowedException()
        }

        // 이미 승인된 현재 기수 멤버는 상태, 팀, 역할, 초기화 데이터를 다시 변경하지 않는다.
        val pendingIds = targets.filter { it.status == MemberStatus.PENDING }.map { it.memberId }
        if (pendingIds.isEmpty()) return
        val roleId = roleQueries.findIdByName(RoleType.Deeper.code)
        roles.replaceCurrentCohortRoles(pendingIds, cohortId.value, roleId)
        teams.replaceCurrentCohortTeams(pendingIds, cohortId.value, null)
        pendingIds.forEach { memberCohorts.save(MemberCohort.of(MemberId(it), cohortId)) }
        members.updateManagementFields(pendingIds, false, null, MemberStatus.ACTIVE, emptySet())

        // 신규 승인 경로는 초기화까지 같은 트랜잭션에 포함한다. 실패하면 승인도 취소된다.
        pendingIds.forEach { initializer.initialize(MemberId(it), cohortId) }
    }
}
