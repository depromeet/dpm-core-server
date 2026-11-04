package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberOAuth

data class MemberMergeTarget(val retainedMemberId: Long, val mergedAt: java.time.Instant)

interface MemberMergePersistencePort {
    /** Only a preserved, withdrawn source with an unambiguous merge event can be resolved. */
    fun findTarget(sourceId: Long): MemberMergeTarget?

    /** Caller holds both member locks. Preserve expiry and rotation metadata. */
    fun transferRefreshTokens(
        sourceId: Long,
        retainedId: Long,
    )

    /** Caller holds member locks in ascending ID order. */
    fun lockOAuths(memberIds: List<Long>): List<MemberOAuth>

    fun hasPasswordCredential(memberIds: List<Long>): Boolean

    fun transferOAuths(
        sourceId: Long,
        retainedId: Long,
    ): Int

    /** Preserve all related records and identity fields. */
    fun softDeleteSource(memberId: Long)

    fun recordMerge(
        actorId: Long,
        retainedId: Long,
        sourceId: Long,
    )
}
