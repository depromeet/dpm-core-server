package core.persistence.member.repository.team

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.entity.member.MemberOAuthEntity
import org.springframework.stereotype.Repository

@Repository
class MemberOAuthRepository(
    private val memberOAuthJpaRepository: MemberOAuthJpaRepository,
) : MemberOAuthPersistencePort {
    override fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    ) {
        memberOAuthJpaRepository.save(MemberOAuthEntity.of(memberOAuth, member))
    }
}
