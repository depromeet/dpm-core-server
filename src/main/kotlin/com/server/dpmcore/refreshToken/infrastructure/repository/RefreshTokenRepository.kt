package com.server.dpmcore.refreshToken.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.deleteQuery
import com.server.dpmcore.common.jdsl.singleQueryOrNull
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.refreshToken.domain.model.RefreshToken
import com.server.dpmcore.refreshToken.domain.port.outbound.RefreshTokenPersistencePort
import com.server.dpmcore.refreshToken.infrastructure.entity.RefreshTokenEntity
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenRepository(
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : RefreshTokenPersistencePort {
    override fun save(refreshToken: RefreshToken): RefreshToken =
        refreshTokenJpaRepository.save(RefreshTokenEntity.from(refreshToken)).toDomain()

    override fun findByMemberId(memberId: MemberId): RefreshToken? =
        queryFactory
            .singleQueryOrNull {
                select(entity(RefreshTokenEntity::class))
                from(entity(RefreshTokenEntity::class))
                where(col(RefreshTokenEntity::memberId).equal(memberId.value))
            }?.toDomain()

    override fun findByToken(token: String): RefreshToken? =
        queryFactory
            .singleQueryOrNull {
                select(entity(RefreshTokenEntity::class))
                from(entity(RefreshTokenEntity::class))
                where(col(RefreshTokenEntity::token).equal(token))
            }?.toDomain()

    override fun deleteByMemberId(memberId: MemberId) {
        queryFactory
            .deleteQuery<RefreshTokenEntity> {
                where(col(RefreshTokenEntity::memberId).equal(memberId.value))
            }.executeUpdate()
    }
}
