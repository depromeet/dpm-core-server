package core.domain.gathering.aggregate

import core.domain.gathering.enums.GatheringCategory
import core.domain.gathering.vo.GatheringV2Id
import java.time.Instant

/**
 * 회식 참여 조사를 위해 사용하는 도메인 모델입니다.
 *
 * - 각 회식에는 제목, 설명, 개최일, 카테고리 등이 포함됩니다.
 * - 회식에 초대된 멤버들은 GatheringV2Invitee로 관리됩니다.
 */
class GatheringV2(
    val id: GatheringV2Id? = null,
    val date: Instant,
    val title: String,
    val description: String? = null,
    val category: GatheringCategory,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
) {
    var updatedAt: Instant? = updatedAt
        private set

    fun update(
        date: Instant? = null,
        title: String? = null,
        description: String? = null,
        category: GatheringCategory? = null,
    ): GatheringV2 =
        GatheringV2(
            id = this.id,
            date = date ?: this.date,
            title = title ?: this.title,
            description = description ?: this.description,
            category = category ?: this.category,
            createdAt = this.createdAt,
            updatedAt = Instant.now(),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GatheringV2) return false
        return id == other.id
    }

    override fun toString(): String =
        "GatheringV2(id=$id, date=$date, title='$title', description=$description, " +
            "category=$category, createdAt=$createdAt, updatedAt=$updatedAt)"

    companion object {
        fun create(
            date: Instant,
            title: String,
            description: String? = null,
            category: GatheringCategory,
        ): GatheringV2 =
            GatheringV2(
                date = date,
                title = title,
                description = description,
                category = category,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
    }
}
