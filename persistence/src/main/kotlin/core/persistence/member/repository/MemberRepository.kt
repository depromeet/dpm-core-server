package core.persistence.member.repository

import core.entity.member.MemberEntity
import core.domain.authority.vo.AuthorityId
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberNameAuthorityQueryModel
import core.domain.member.vo.MemberId
import org.jooq.dsl.tables.references.AUTHORITIES
import org.jooq.dsl.tables.references.COHORTS
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_AUTHORITIES
import org.jooq.dsl.tables.references.MEMBER_COHORTS
import org.jooq.dsl.tables.references.MEMBER_TEAMS
import org.jooq.dsl.tables.references.TEAMS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class MemberRepository(
    private val memberJpaRepository: MemberJpaRepository,
    private val dsl: DSLContext,
) : MemberPersistencePort {
    override fun findByEmail(email: String): Member? = memberJpaRepository.findByEmail(email).toDomain()

    override fun save(member: Member): Member = memberJpaRepository.save(MemberEntity.from(member)).toDomain()

    override fun findById(memberId: Long): Member? = memberJpaRepository.findById(memberId).orElse(null).toDomain()

    override fun existsById(memberId: Long): Boolean = memberJpaRepository.existsById(memberId)

    override fun existsDeletedMemberById(memberId: Long): Boolean =
        memberJpaRepository.existsByIdAndDeletedAtIsNotNull(memberId)

    override fun findAllMemberIdByAuthorityIds(authorityIds: List<AuthorityId>): List<MemberId> =
        dsl
            .selectDistinct(MEMBERS.MEMBER_ID)
            .from(MEMBERS)
            .join(MEMBER_AUTHORITIES)
            .on(MEMBERS.MEMBER_ID.eq(MEMBER_AUTHORITIES.MEMBER_ID))
            .join(AUTHORITIES)
            .on(MEMBER_AUTHORITIES.AUTHORITY_ID.eq(AUTHORITIES.AUTHORITY_ID))
            .where(AUTHORITIES.AUTHORITY_ID.`in`(authorityIds.map { it.value }))
            .fetch(MEMBERS.MEMBER_ID)
            .map { MemberId(it ?: 0L) }

    override fun findAllByCohort(value: String): List<MemberId> =
        dsl
            .select(MEMBERS.MEMBER_ID)
            .from(MEMBERS)
            .join(MEMBER_COHORTS)
            .on(MEMBERS.MEMBER_ID.eq(MEMBER_COHORTS.MEMBER_ID))
            .join(COHORTS)
            .on(MEMBER_COHORTS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .where(COHORTS.VALUE.eq(value))
            .fetch(MEMBERS.MEMBER_ID)
            .filterNotNull()
            .map {
                MemberId(it)
            }

    override fun findMemberNameAndAuthorityByMemberId(memberId: MemberId): List<MemberNameAuthorityQueryModel> =
        dsl.select(MEMBERS.NAME, AUTHORITIES.NAME)
            .from(MEMBERS)
            .join(MEMBER_AUTHORITIES).on(MEMBERS.MEMBER_ID.eq(MEMBER_AUTHORITIES.MEMBER_ID))
            .join(AUTHORITIES).on(MEMBER_AUTHORITIES.AUTHORITY_ID.eq(AUTHORITIES.AUTHORITY_ID))
            .where(MEMBERS.MEMBER_ID.eq(memberId.value))
            .fetch()
            .mapNotNull { (memberName, authorityName) ->
                memberName?.let { name ->
                    authorityName?.let { authority ->
                        MemberNameAuthorityQueryModel(name, authority)
                    }
                }
            }

    override fun findMemberTeamByMemberId(memberId: MemberId): Int? =
        dsl
            .select(TEAMS.NUMBER)
            .from(MEMBER_TEAMS)
            .join(MEMBERS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(MEMBER_TEAMS.MEMBER_ID.eq(memberId.value))
            .fetchOne(TEAMS.NUMBER)
}
