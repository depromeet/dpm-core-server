package core.application.member.application.service.role

import core.domain.authorization.vo.RoleType
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberRoleAssignment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class CurrentCohortRoleResolverTest {
    private val resolver =
        CurrentCohortRoleResolver(
            cohortQueryService = mock(core.application.cohort.application.service.CohortQueryService::class.java),
            memberCohortPersistencePort = mock(core.domain.member.port.outbound.MemberCohortPersistencePort::class.java),
            memberRolePersistencePort = mock(core.domain.member.port.outbound.MemberRolePersistencePort::class.java),
            activities = mock(core.domain.member.port.outbound.MemberActivityPersistencePort::class.java),
        )

    @Test
    fun guestIsAlwaysEffective() {
        val context = context(18L, setOf(18L))
        assertEquals(
            listOf("GUEST"),
            resolver.filterEffectiveRoles(listOf(MemberRoleAssignment("GUEST", null)), context),
        )
    }

    @Test
    fun alumniOrganizerFilteredWhenNotInActiveCohort() {
        val context = context(18L, setOf(17L))
        assertEquals(
            emptyList<String>(),
            resolver.filterEffectiveRoles(listOf(MemberRoleAssignment("ORGANIZER", CohortId(17L))), context),
        )
    }

    @Test
    fun deeperInActiveCohortIsEffective() {
        val context = context(18L, setOf(18L))
        assertEquals(listOf("DEEPER"), resolver.filterEffectiveRoles(listOf(MemberRoleAssignment("DEEPER", CohortId(18L))), context))
    }

    @Test
    fun masterIsAlwaysEffective() {
        val context = context(18L, emptySet())
        assertEquals(listOf("MASTER"), resolver.filterEffectiveRoles(listOf(MemberRoleAssignment("MASTER", null)), context))
    }

    @Test
    fun coreIsFilteredWhenNotInActiveCohort() {
        val context = context(18L, setOf(17L))
        assertEquals(
            emptyList<String>(),
            resolver.filterEffectiveRoles(listOf(MemberRoleAssignment("CORE", CohortId(17L))), context),
        )
    }

    @Test
    fun coreInActiveCohortIsEffective() {
        val context = context(18L, setOf(18L))
        assertEquals(
            listOf("CORE"),
            resolver.filterEffectiveRoles(listOf(MemberRoleAssignment("CORE", CohortId(18L))), context),
        )
    }

    @Test
    fun primaryRoleFallsBackToGuest() {
        val context = context(18L, setOf(17L))
        assertEquals(RoleType.Guest, resolver.findPrimaryRoleType(listOf(MemberRoleAssignment("DEEPER", CohortId(17L))), context))
    }

    @Test
    fun suspendedAndEndedSuppressEveryAssignedRoleIncludingMasterAndGuest() {
        val roles = listOf("MASTER", "CORE", "ORGANIZER", "DEEPER", "GUEST").map { MemberRoleAssignment(it, CohortId(19)) }
        listOf(core.domain.member.enums.MemberActivityStatus.SUSPENDED, core.domain.member.enums.MemberActivityStatus.ENDED).forEach { status ->
            listOf(setOf(19L), emptySet()).forEach { memberships ->
                assertEquals(emptyList<String>(), resolver.filterEffectiveRoles(roles, CurrentCohortRoleResolver.CohortRoleContext(19, memberships, status)))
            }
        }
    }

    @Test
    fun missingActivityFailsClosedOnlyForCurrentMembers() {
        val roles = listOf(MemberRoleAssignment("MASTER", null))
        assertEquals(emptyList<String>(), resolver.filterEffectiveRoles(roles, CurrentCohortRoleResolver.CohortRoleContext(19, setOf(19L), null)))
        assertEquals(listOf("MASTER"), resolver.filterEffectiveRoles(roles, CurrentCohortRoleResolver.CohortRoleContext(19, emptySet(), null)))
    }

    @Test
    fun bulkAdminResolutionLoadsActivityAndMembershipOnceAndMatchesSingleRoleRules() {
        val cohorts = mock(core.application.cohort.application.service.CohortQueryService::class.java)
        val memberships = mock(core.domain.member.port.outbound.MemberCohortPersistencePort::class.java)
        val roles = mock(core.domain.member.port.outbound.MemberRolePersistencePort::class.java)
        val activities = mock(core.domain.member.port.outbound.MemberActivityPersistencePort::class.java)
        val batch = CurrentCohortRoleResolver(cohorts, memberships, roles, activities)
        org.mockito.Mockito.`when`(cohorts.getActiveCohort()).thenReturn(core.domain.cohort.aggregate.Cohort(CohortId(19), "19", isActive = true))
        val ids = listOf(1L, 2L, 3L)
        org.mockito.Mockito.`when`(roles.findActiveRoleAssignmentsByMemberIds(ids)).thenReturn(
            ids.associateWith {
                listOf(MemberRoleAssignment("MASTER", null))
            },
        )
        org.mockito.Mockito.`when`(memberships.findCohortIdsByMemberIds(ids)).thenReturn(mapOf(1L to listOf(19L), 2L to listOf(19L)))
        org.mockito.Mockito.`when`(activities.findAll(ids, 19)).thenReturn(
            mapOf(
                1L to core.domain.member.port.outbound.MemberCohortActivity(1, 19, core.domain.member.enums.MemberActivityStatus.ACTIVE),
                2L to core.domain.member.port.outbound.MemberCohortActivity(2, 19, core.domain.member.enums.MemberActivityStatus.SUSPENDED),
            ),
        )
        assertEquals(
            mapOf(core.domain.member.vo.MemberId(1) to RoleType.Master, core.domain.member.vo.MemberId(2) to RoleType.Guest, core.domain.member.vo.MemberId(3) to RoleType.Master),
            batch.findPrimaryRoleTypesForMembers(ids.map(::coreMemberId)),
        )
        org.mockito.Mockito.verify(activities).findAll(ids, 19)
        org.mockito.Mockito.verify(memberships).findCohortIdsByMemberIds(ids)
        org.mockito.Mockito.verifyNoMoreInteractions(activities, memberships)
    }

    private fun coreMemberId(id: Long) = core.domain.member.vo.MemberId(id)

    private fun context(
        activeCohortId: Long?,
        memberCohortIds: Set<Long>,
    ) = CurrentCohortRoleResolver.CohortRoleContext(activeCohortId, memberCohortIds)
}
