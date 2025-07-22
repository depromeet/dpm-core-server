package com.server.dpmcore.gathering.gathering.infrastructure.entity

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gathering.domain.model.GatheringCategory
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.infrastructure.entity.GatheringMemberEntity
import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.GatheringReceiptEntity
import com.server.dpmcore.member.member.domain.model.MemberId
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "gatherings")
class GatheringEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gathering_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "title", nullable = false)
    val title: String,
    @Column(name = "description")
    val description: String? = null,
    @Column(name = "held_at", nullable = false)
    val heldAt: Instant,
    @Column(name = "category", nullable = false)
    val category: String,
    @Column(name = "host_user_id", nullable = false)
    val hostUserId: Long,
    @Column(name = "round_number", nullable = false)
    val roundNumber: Int,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val bill: BillEntity? = null,
    @OneToMany(mappedBy = "gathering", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val gatheringMembers: MutableList<GatheringMemberEntity> = mutableListOf(),
    @OneToOne(mappedBy = "gathering", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val receipt: GatheringReceiptEntity? = null,
) {
    companion object {
        fun from(
            bill: Bill,
            gathering: Gathering,
        ): GatheringEntity =
            GatheringEntity(
                id = gathering.id?.value ?: 0L,
                title = gathering.title,
                description = gathering.description,
                heldAt = gathering.heldAt,
                category = gathering.category.value,
                hostUserId = gathering.hostUserId.value,
                roundNumber = gathering.roundNumber,
                createdAt = gathering.createdAt ?: Instant.now(),
                updatedAt = gathering.updatedAt ?: Instant.now(),
                deletedAt = gathering.deletedAt,
                bill = BillEntity.from(bill),
                receipt = gathering.gatheringReceipt?.let { GatheringReceiptEntity.from(it) },
            )

        fun from(gathering: Gathering): GatheringEntity =
            GatheringEntity(
                id = gathering.id?.value ?: 0L,
                title = gathering.title,
                description = gathering.description,
                heldAt = gathering.heldAt,
                category = gathering.category.value,
                hostUserId = gathering.hostUserId.value,
                roundNumber = gathering.roundNumber,
                createdAt = gathering.createdAt ?: Instant.now(),
                updatedAt = gathering.updatedAt ?: Instant.now(),
                deletedAt = gathering.deletedAt,
                receipt = gathering.gatheringReceipt?.let { GatheringReceiptEntity.from(it) },
            )
    }

    fun toDomain(): Gathering =
        Gathering(
            id = GatheringId(id),
            title = title,
            description = description,
            heldAt = heldAt,
            category = GatheringCategory.valueOf(category),
            hostUserId = MemberId(hostUserId),
            roundNumber = roundNumber,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            billId = BillId(bill?.id ?: throw BillException.BillIdRequiredException()),
            gatheringMembers = gatheringMembers.map { it.toDomain() }.toMutableList(),
            gatheringReceipt = receipt?.toDomain(),
        )
}
