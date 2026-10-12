package core.application.member.application.service

import core.application.member.application.exception.InvalidMemberManagementTeamException
import core.application.member.application.exception.InvalidMemberManagementUpdateException
import core.application.member.application.exception.MemberManagementTargetNotAllowedException
import core.application.member.presentation.request.MemberManagementBulkUpdateRequest
import core.application.member.presentation.request.MemberManagementUpdateRequest
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.port.outbound.MemberTeamPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
class MemberManagementCommandService(
    private val members: MemberPersistencePort,
    private val teams: MemberTeamPersistencePort,
    private val roles: MemberRolePersistencePort,
    private val cohorts: CohortQueryUseCase,
    private val cohortPersistencePort: CohortPersistencePort,
    private val roleQueryUseCase: RoleQueryUseCase,
) {
    @TrackMemberBadges
    fun update(
        memberId: Long,
        request: MemberManagementUpdateRequest,
    ) {
        updateMembers(listOf(memberId), request, bulk = false)
    }

    @TrackMemberBadges
    fun updateBulk(request: MemberManagementBulkUpdateRequest) {
        updateMembers(request.memberIds, request.changes, bulk = true)
    }

    private fun updateMembers(
        memberIds: List<Long?>,
        changes: MemberManagementUpdateRequest,
        bulk: Boolean,
    ) {
        val fieldCount = listOf(changes.part, changes.teamId, changes.memberType, changes.status).count { it != null }
        if (fieldCount == 0 || (bulk && fieldCount != 1) ||
            memberIds.isEmpty() || memberIds.any { it == null || it <= 0 } ||
            memberIds.distinct().size != memberIds.size ||
            (changes.part != null && changes.part !in PARTS) ||
            (changes.teamId != null && changes.teamId < 0) ||
            (changes.memberType != null && changes.memberType !in TYPES) ||
            (changes.status != null && changes.status !in STATUSES)
        ) {
            throw InvalidMemberManagementUpdateException()
        }

        val ids = memberIds.filterNotNull().sorted()
        val cohortId = cohorts.getActiveCohortId()
        // 모든 대상의 잠금을 같은 순서로 획득하고, 검증이 끝나기 전에는 변경하지 않는다.
        if (members.lockApprovedManagementMemberIds(ids, cohortId.value) != ids) {
            throw MemberManagementTargetNotAllowedException()
        }
        val teamId = changes.teamId?.takeIf { it != 0L }
        if (teamId != null && cohortPersistencePort.findTeamsByCohortId(cohortId).none { it.id == teamId }) {
            throw InvalidMemberManagementTeamException()
        }
        val roleId = changes.memberType?.takeUnless { it == UNASSIGNED }?.let(roleQueryUseCase::findIdByName)

        val changedAssociationMemberIds = mutableSetOf<Long>()
        if (changes.teamId != null) {
            changedAssociationMemberIds += teams.replaceCurrentCohortTeams(ids, cohortId.value, teamId)
        }
        if (changes.memberType != null) {
            changedAssociationMemberIds += roles.replaceCurrentCohortRoles(ids, cohortId.value, roleId)
        }
        members.updateManagementFields(
            memberIds = ids,
            updatePart = changes.part != null,
            part = changes.part?.takeUnless { it == UNASSIGNED }?.let(MemberPart::valueOf),
            status = changes.status?.let(MemberStatus::valueOf),
            changedAssociationMemberIds = changedAssociationMemberIds,
        )
    }

    private companion object {
        const val UNASSIGNED = "UNASSIGNED"
        val PARTS = MemberPart.entries.map { it.name }.toSet() + UNASSIGNED
        val TYPES = setOf("DEEPER", "ORGANIZER", "CORE", UNASSIGNED)
        val STATUSES = setOf("ACTIVE", "INACTIVE")
    }
}
