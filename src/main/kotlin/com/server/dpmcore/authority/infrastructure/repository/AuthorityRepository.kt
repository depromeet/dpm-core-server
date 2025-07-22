package com.server.dpmcore.authority.infrastructure.repository

import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.listQuery
import com.server.dpmcore.authority.domain.model.Authority
import com.server.dpmcore.authority.domain.port.AuthorityPersistencePort
import com.server.dpmcore.authority.infrastructure.entity.AuthorityEntity
import org.springframework.stereotype.Repository

@Repository
class AuthorityRepository(
    private val authorityJpaRepository: AuthorityJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : AuthorityPersistencePort {
    override fun findAll(): List<Authority> =
        queryFactory
            .listQuery {
                select(entity(AuthorityEntity::class))
                from(entity(AuthorityEntity::class))
            }.mapNotNull { it?.toDomain() }
}
