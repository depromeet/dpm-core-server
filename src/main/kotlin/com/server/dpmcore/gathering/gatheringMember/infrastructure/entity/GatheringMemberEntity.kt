package com.server.dpmcore.gathering.gatheringMember.infrastructure.entity

import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMemberId
import com.server.dpmcore.member.member.domain.model.MemberId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "gathering_members")
class GatheringMemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gathering_member_id", nullable = false, updatable = false)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false)
    val gathering: GatheringEntity,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "is_checked", nullable = false)
    val isChecked: Boolean = false,
    @Column(name = "is_joined", nullable = false)
    val isJoined: Boolean = false,
    @Column(name = "completed_at")
    val completedAt: Instant? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
) {
    companion object {
        fun from(gatheringMember: GatheringMember): GatheringMemberEntity =
            GatheringMemberEntity(
                id = gatheringMember.id?.value ?: 0L,
                gathering = GatheringEntity.from(gatheringMember.gathering!!),
                memberId = gatheringMember.memberId.value,
                isChecked = gatheringMember.isChecked,
                isJoined = gatheringMember.isJoined,
                completedAt = gatheringMember.completedAt,
                createdAt = gatheringMember.createdAt ?: Instant.now(),
                updatedAt = gatheringMember.updatedAt ?: Instant.now(),
                deletedAt = gatheringMember.deletedAt,
            )
    }

    fun toDomain(): GatheringMember =
        GatheringMember(
            id = GatheringMemberId(id),
            gathering = gathering.toDomain(),
            memberId = MemberId(memberId),
            isChecked = isChecked,
            isJoined = isJoined,
            completedAt = completedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
}
