package com.server.dpmcore.member.member.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.common.jdsl.singleQueryOrNull
import com.server.dpmcore.member.member.application.exception.MemberNameAuthorityCanNotBeNullException
import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.port.inbound.query.MemberNameAuthorityQueryModel
import com.server.dpmcore.member.member.domain.port.outbound.MemberPersistencePort
import com.server.dpmcore.member.member.infrastructure.entity.MemberEntity
import org.jooq.DSLContext
import org.jooq.generated.tables.references.AUTHORITIES
import org.jooq.generated.tables.references.COHORTS
import org.jooq.generated.tables.references.MEMBERS
import org.jooq.generated.tables.references.MEMBER_AUTHORITIES
import org.jooq.generated.tables.references.MEMBER_COHORTS
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class MemberRepository(
    private val memberJpaRepository: MemberJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
    private val dsl: DSLContext,
) : MemberPersistencePort {
    override fun findByEmail(email: String): Member? =
        queryFactory
            .singleQueryOrNull {
                select(entity(MemberEntity::class))
                from(entity(MemberEntity::class))
                where(col(MemberEntity::email).equal(email))
            }?.toDomain()

    override fun save(member: Member): Member = memberJpaRepository.save(MemberEntity.from(member)).toDomain()

    override fun findById(memberId: Long): Member? =
        queryFactory
            .singleQueryOrNull {
                select(entity(MemberEntity::class))
                from(entity(MemberEntity::class))
                where(col(MemberEntity::id).equal(memberId))
            }?.toDomain()

    override fun existsById(memberId: Long): Boolean =
        queryFactory
            .singleQueryOrNull {
                select(col(MemberEntity::id))
                from(entity(MemberEntity::class))
                where(col(MemberEntity::id).equal(memberId))
            } != null

    override fun delete(memberId: Long) {
        queryFactory
            .updateQuery(MemberEntity::class) {
                set(col(MemberEntity::deletedAt), Instant.now())
                where(col(MemberEntity::id).equal(memberId))
            }.executeUpdate()
    }

    override fun existsDeletedMemberById(memberId: Long): Boolean =
        queryFactory
            .singleQueryOrNull {
                select(col(MemberEntity::id))
                from(entity(MemberEntity::class))
                where(
                    col(MemberEntity::id)
                        .equal(memberId)
                        .and(col(MemberEntity::deletedAt).isNotNull()),
                )
            } != null

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

    override fun findMemberNameAndAuthorityByMemberId(memberId: MemberId): MemberNameAuthorityQueryModel {
        val record =
            dsl
                .select(MEMBERS.NAME, AUTHORITIES.NAME)
                .from(MEMBERS)
                .join(MEMBER_AUTHORITIES)
                .on(MEMBERS.MEMBER_ID.eq(MEMBER_AUTHORITIES.MEMBER_ID))
                .join(AUTHORITIES)
                .on(MEMBER_AUTHORITIES.AUTHORITY_ID.eq(AUTHORITIES.AUTHORITY_ID))
                .where(MEMBERS.MEMBER_ID.eq(memberId.value))
                .fetchOne() ?: throw MemberNameAuthorityCanNotBeNullException()

        val memberName = record.get(MEMBERS.NAME)
        val authorityName = record.get(AUTHORITIES.NAME)

        if (memberName == null || authorityName == null) {
            throw MemberNameAuthorityCanNotBeNullException()
        }

        return MemberNameAuthorityQueryModel(
            name = memberName,
            authority = authorityName,
        )
    }
}
