package com.server.dpmcore.gathering.gatheringReceiptPhoto.infrastructure.entity

import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.ReceiptEntity
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
@Table(name = "receipt_photos")
class ReceiptPhotoEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_photo_id", nullable = false, updatable = false)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    val receipt: ReceiptEntity,

    @Column(name = "url", nullable = false)
    val url: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null
)
