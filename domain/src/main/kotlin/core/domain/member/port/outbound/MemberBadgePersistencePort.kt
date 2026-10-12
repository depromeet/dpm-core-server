package core.domain.member.port.outbound

/** Every mutation follows lockStates, in the caller's transaction. */
interface MemberBadgePersistencePort {
    fun findStates(cohortId: Long): List<MemberBadgeState>

    fun lockStates(cohortId: Long): List<MemberBadgeState>

    fun findMembers(
        cohortId: Long,
        card: String,
    ): Set<Long>

    fun replaceMembers(
        cohortId: Long,
        card: String,
        previous: Set<Long>,
        members: Set<Long>,
    )

    fun save(state: MemberBadgeState)
}

data class MemberBadgeState(
    val cohortId: Long,
    val card: String,
    val version: Long = 0,
    val acknowledgedVersion: Long = 0,
    val targetCount: Int = 0,
    val initialized: Boolean = false,
)
