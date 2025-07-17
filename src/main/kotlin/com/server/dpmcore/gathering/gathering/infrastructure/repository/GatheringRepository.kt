package com.server.dpmcore.gathering.gathering.infrastructure.repository

import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.singleQuery
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.port.GatheringPersistencePort
import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import org.springframework.stereotype.Repository

@Repository
class GatheringRepository(
    private val gatheringJpaRepository: GatheringJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : GatheringPersistencePort {
    override fun save(gathering: Gathering): Gathering =
        gatheringJpaRepository.save(GatheringEntity.from(gathering)).toDomain()

    override fun findGatheringById(id: Long) =
        queryFactory
            .singleQuery<GatheringEntity> {
                select(entity(GatheringEntity::class))
                from(entity(GatheringEntity::class))
//                where(GatheringEntity::id id)
            }.toDomain()
}
