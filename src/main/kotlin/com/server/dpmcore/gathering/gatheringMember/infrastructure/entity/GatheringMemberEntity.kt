package com.server.dpmcore.gathering.gatheringMember.infrastructure.entity

import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import jakarta.persistence.*
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

    @Column(name = "is_completed", nullable = false)
    val isCompleted: Boolean = false,

    @Column(name = "completed_at")
    val completedAt: Instant? = null
)
