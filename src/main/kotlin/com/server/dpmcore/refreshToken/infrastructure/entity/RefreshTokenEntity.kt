package com.server.dpmcore.refreshToken.infrastructure.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    val memberId: Long,

    @Lob
    @Column(columnDefinition = "TEXT")
    var token: String
)
