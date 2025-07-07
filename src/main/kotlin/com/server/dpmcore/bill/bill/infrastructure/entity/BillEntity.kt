package com.server.dpmcore.bill.bill.infrastructure.entity

import com.server.dpmcore.bill.billAccount.infrastructure.entity.BillAccountEntity
import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "bills")
class BillEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_id", nullable = false, updatable = false)
    val id: Long,


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_account_id", nullable = false)
    val billAccount: BillAccountEntity,

    @OneToMany(mappedBy = "bill", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val gatherings: MutableList<GatheringEntity> = mutableListOf(),

    @Column(name = "completed_at")
    val completedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null
)
