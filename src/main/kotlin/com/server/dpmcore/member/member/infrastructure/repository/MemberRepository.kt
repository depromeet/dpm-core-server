package com.server.dpmcore.member.member.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.server.dpmcore.common.jdsl.singleQueryOrNull
import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.member.domain.port.outbound.MemberPersistencePort
import com.server.dpmcore.member.member.infrastructure.entity.MemberEntity
import com.server.dpmcore.member.member.infrastructure.mapper.MemberMapper
import org.springframework.stereotype.Repository

@Repository
class MemberRepository(
    private val memberJpaRepository: MemberJpaRepository,
    private val memberMapper: MemberMapper,
    private val queryFactory: SpringDataQueryFactory,
) : MemberPersistencePort {

    override fun findByEmail(email: String): Member? {
        return queryFactory.singleQueryOrNull {
            select(entity(MemberEntity::class))
            from(entity(MemberEntity::class))
            where(col(MemberEntity::email).equal(email))
        }?.let { entity ->
            memberMapper.toDomain(entity)
        }
    }

    override fun save(member: Member): Long {
        return memberJpaRepository.save(memberMapper.toEntity(member)).id
    }
}
