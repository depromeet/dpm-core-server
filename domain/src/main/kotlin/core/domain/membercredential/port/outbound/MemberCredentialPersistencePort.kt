package core.domain.membercredential.port.outbound

import core.domain.membercredential.aggregate.MemberCredential
import core.domain.membercredential.aggregate.MemberCredentialId
import core.domain.member.vo.MemberId
import java.time.Instant

interface MemberCredentialPersistencePort {
    fun save(credential: MemberCredential): MemberCredential
    fun findByEmail(email: String): MemberCredential?
    fun findByMemberId(memberId: MemberId): MemberCredential?
    fun existsByEmail(email: String): Boolean
    fun updatePassword(
        credentialId: MemberCredentialId,
        encodedPassword: String,
        updatedAt: Instant,
    )

    fun deleteByMemberId(memberId: MemberId)
}
