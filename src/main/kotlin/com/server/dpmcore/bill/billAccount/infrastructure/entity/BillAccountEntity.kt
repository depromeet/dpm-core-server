package com.server.dpmcore.bill.billAccount.infrastructure.entity

import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "bill_accounts")
class BillAccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_account_id", nullable = false, updatable = false)
    val id: Long = 0,

    @Column(name = "bill_account_info", nullable = false)
    val billAccountInfo: String,

    @Column(name = "is_url", nullable = false)
    val isUrl: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,

    @OneToMany(mappedBy = "billAccount", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val bills: MutableList<BillEntity> = mutableListOf()
)

