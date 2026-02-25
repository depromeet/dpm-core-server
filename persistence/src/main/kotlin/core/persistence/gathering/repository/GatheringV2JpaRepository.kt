package core.persistence.gathering.repository

import core.entity.gathering.GatheringV2Entity
import org.springframework.data.jpa.repository.JpaRepository

interface GatheringV2JpaRepository : JpaRepository<GatheringV2Entity, Long> {
    fun findAllBy(): List<GatheringV2Entity>
}
