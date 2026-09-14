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

    private fun context(
        activeCohortId: Long?,
        memberCohortIds: Set<Long>,
    ) = CurrentCohortRoleResolver.CohortRoleContext(activeCohortId, memberCohortIds)
}
