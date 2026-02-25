package core.persistence.membercredential.repository

import core.domain.member.vo.MemberId
import core.domain.membercredential.aggregate.MemberCredential
import core.domain.membercredential.aggregate.MemberCredentialId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.entity.membercredential.MemberCredentialEntity
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class MemberCredentialRepository(
    private val memberCredentialJpaRepository: MemberCredentialJpaRepository,
) : MemberCredentialPersistencePort {
    override fun save(credential: MemberCredential): MemberCredential =
        memberCredentialJpaRepository.save(MemberCredentialEntity.from(credential)).toDomain()

    override fun findByEmail(email: String): MemberCredential? =
        memberCredentialJpaRepository.findByEmail(email)?.toDomain()

    override fun findByMemberId(memberId: MemberId): MemberCredential? =
        memberCredentialJpaRepository.findByMemberId(memberId.value)?.toDomain()

    override fun existsByEmail(email: String): Boolean = memberCredentialJpaRepository.existsByEmail(email)

    override fun updatePassword(
        credentialId: MemberCredentialId,
        encodedPassword: String,
        updatedAt: Instant,
    ) {
        val entity =
            memberCredentialJpaRepository.findById(credentialId.value).orElseThrow {
                IllegalArgumentException("MemberCredential not found with id: $credentialId")
            }
        entity.password = encodedPassword
        entity.updatedAt = updatedAt
        memberCredentialJpaRepository.save(entity)
    }

    override fun deleteByMemberId(memberId: MemberId) {
        val entity =
            memberCredentialJpaRepository.findByMemberId(memberId.value)
                ?: throw IllegalArgumentException("MemberCredential not found for memberId: $memberId")
        memberCredentialJpaRepository.delete(entity)
    }
}
