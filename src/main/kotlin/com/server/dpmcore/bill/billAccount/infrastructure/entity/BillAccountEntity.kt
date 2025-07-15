package com.server.dpmcore.bill.billAccount.infrastructure.entity

import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import com.server.dpmcore.bill.billAccount.domain.model.AccountType
import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.model.BillAccountId
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "bill_accounts")
class BillAccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_account_id", nullable = false, updatable = false)
    val id: Long = 0,
    @Column(name = "bill_account_value", nullable = false)
    val billAccountValue: String,
    @Column(name = "account_holder_name", nullable = false)
    val accountHolderName: String,
    @Column(name = "bank_name", nullable = false)
    val bankName: String,
    @Column(name = "account_type", nullable = false)
    val accountType: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
    @OneToMany(mappedBy = "billAccount", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val bills: MutableList<BillEntity> = mutableListOf(),
) {
    companion object {
        fun from(billAccount: BillAccount): BillAccountEntity {
            return BillAccountEntity(
                id = billAccount.id?.value ?: 0L,
                billAccountValue = billAccount.billAccountValue,
                accountHolderName = billAccount.accountHolderName,
                bankName = billAccount.bankName,
                accountType = billAccount.accountType.value,
                createdAt = billAccount.createdAt ?: Instant.now(),
                updatedAt = billAccount.updatedAt ?: Instant.now(),
                deletedAt = billAccount.deletedAt,
            )
        }
    }

    fun toDomain(): BillAccount {
        return BillAccount(
            id = BillAccountId(id),
            billAccountValue = billAccountValue,
            accountHolderName = accountHolderName,
            bankName = bankName,
            accountType = AccountType.valueOf(accountType),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
    }
}
