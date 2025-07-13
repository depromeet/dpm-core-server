package com.server.dpmcore.refreshToken.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
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

    override fun save(refreshToken: RefreshToken): RefreshToken {
        return refreshTokenJpaRepository.save(RefreshTokenEntity.toEntity(refreshToken)).toDomain()
    }

    override fun findByMemberId(memberId: MemberId): RefreshToken? {
        return queryFactory.singleQueryOrNull {
            select(entity(RefreshToken::class))
            from(entity(RefreshToken::class))
            where(col(RefreshToken::memberId).equal(memberId))
        }?.let { entity ->
            RefreshToken.create(entity.memberId, entity.token)
        }
    }
}
