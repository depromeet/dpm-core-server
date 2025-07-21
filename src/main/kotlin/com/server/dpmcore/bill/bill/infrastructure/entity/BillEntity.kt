package com.server.dpmcore.bill.bill.infrastructure.entity

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.billAccount.infrastructure.entity.BillAccountEntity
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.infrastructure.entity.GatheringEntity
import com.server.dpmcore.member.member.domain.model.MemberId
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
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
    @Column(name = "title", nullable = false)
    val title: String,
    @Column(name = "description")
    val description: String,
    @Column(name = "host_user_id")
    val hostUserId: Long,
    @OneToMany(mappedBy = "bill", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val gatherings: MutableList<GatheringEntity> = mutableListOf(),
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
        fun from(bill: Bill): BillEntity =
            BillEntity(
                id = bill.id?.value ?: 0L,
                billAccount = BillAccountEntity.from(bill.billAccount),
                title = bill.title,
                description = bill.description ?: "",
                hostUserId = bill.hostUserId.value,
                completedAt = bill.completedAt,
                createdAt = bill.createdAt ?: Instant.now(),
                updatedAt = bill.updatedAt ?: Instant.now(),
                deletedAt = bill.deletedAt,
            )
    }

    fun toDomain(): Bill =
        Bill(
            id = BillId(id),
            billAccount = billAccount.toDomain(),
            title = title,
            description = description,
            hostUserId = MemberId(hostUserId),
            gatheringIds = gatherings.map { GatheringId(it.id) }.toMutableList(),
            completedAt = completedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
}
