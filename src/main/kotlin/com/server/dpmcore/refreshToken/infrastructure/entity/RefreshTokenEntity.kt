package com.server.dpmcore.refreshToken.infrastructure.entity

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.refreshToken.domain.model.RefreshToken
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
    var token: String,
) {
    fun toDomain(): RefreshToken =
        RefreshToken(
            memberId = MemberId(this.memberId),
            token = this.token,
        )

    companion object {
        fun from(domain: RefreshToken) =
            RefreshTokenEntity(
                memberId = domain.memberId.value,
                token = domain.token,
            )
    }
}
