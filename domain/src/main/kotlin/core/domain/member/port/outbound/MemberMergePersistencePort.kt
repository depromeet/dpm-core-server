package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberOAuth

interface MemberMergePersistencePort {
    /** Caller holds member locks in ascending ID order. */
    fun lockOAuths(memberIds: List<Long>): List<MemberOAuth>

    fun hasPasswordCredential(memberIds: List<Long>): Boolean

    fun transferOAuths(
        sourceId: Long,
        retainedId: Long,
    ): Int

    /** Preserve all related records and identity fields. */
    fun softDeleteSource(memberId: Long)
}
