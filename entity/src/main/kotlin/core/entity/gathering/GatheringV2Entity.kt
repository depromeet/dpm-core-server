package core.entity.gathering

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.enums.GatheringCategory
import core.domain.gathering.vo.GatheringV2Id
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "gatherings_v2")
class GatheringV2Entity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gathering_id", nullable = false, updatable = false)
    val id: Long = 0,
    @Column(name = "date", nullable = false)
    val date: Instant,
    @Column(name = "title", nullable = false, length = 200)
    val title: String,
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,
    @Column(name = "category", nullable = false, length = 50)
    val category: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
) {
    companion object {
        fun from(gatheringV2: GatheringV2): GatheringV2Entity =
            GatheringV2Entity(
                date = gatheringV2.date,
                title = gatheringV2.title,
                description = gatheringV2.description,
                category = gatheringV2.category.name,
                createdAt = gatheringV2.createdAt ?: Instant.now(),
                updatedAt = gatheringV2.updatedAt ?: Instant.now(),
            )
    }

    fun toDomain(): GatheringV2 =
        GatheringV2(
            id = GatheringV2Id(this.id),
            date = this.date,
            title = this.title,
            description = this.description,
            category = GatheringCategory.valueOf(this.category),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
        )
}
