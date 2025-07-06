package com.server.dpmcore.gathering.gathering.infrastructure.entity

import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "gatherings")
class GatheringEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gathering_id", nullable = false, updatable = false)
    val id: Long = 0,

    @Column(name = "title", nullable = false)
    val title: String,

    @Column(name = "description")
    val description: String,

    @Column(name = "held_at", nullable = false)
    val heldAt: Instant,

    @Column(name = "category", nullable = false)
    val category: String,

    @Column(name = "host_user_id", nullable = false)
    val hostUserId: Long,

    @Column(name = "round_number", nullable = false)
    val roundNumber: Int,

    @Column(name = "is_completed", nullable = false)
    val isCompleted: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    val bill: BillEntity,

    @OneToMany(mappedBy = "gathering", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val gatheringMembers: MutableList<GatheringMemberEntity> = mutableListOf()

)
