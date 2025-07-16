package com.server.dpmcore.member.member.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.server.dpmcore.common.jdsl.singleQueryOrNull
import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.member.domain.port.outbound.MemberPersistencePort
import com.server.dpmcore.member.member.infrastructure.entity.MemberEntity
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class MemberRepository(
    private val memberJpaRepository: MemberJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : MemberPersistencePort {
    override fun findByEmail(email: String): Member? =
        queryFactory
            .singleQueryOrNull {
                select(entity(MemberEntity::class))
                from(entity(MemberEntity::class))
                where(col(MemberEntity::email).equal(email))
            }?.toDomain()

    override fun save(member: Member): Long = memberJpaRepository.save(MemberEntity.from(member)).id

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
}
