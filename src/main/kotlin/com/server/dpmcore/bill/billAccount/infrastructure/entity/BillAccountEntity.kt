package com.server.dpmcore.bill.billAccount.infrastructure.entity

import jakarta.persistence.*

@Entity
@Table(name = "bill_accounts")
class BillAccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_account_row_id", nullable = false, updatable = false)
    val id: Long = 0,

    @Column(name = "bill_account_info", nullable = false)
    val billAccountInfo: String,

    @Column(name = "is_url", nullable = false)
    val isUrl: Boolean = false
)

