package core.domain.member.aggregate

import core.domain.member.enums.OAuthProvider
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId

class MemberOAuth(
    val id: MemberOAuthId? = null,
    val externalId: String,
    val provider: OAuthProvider,
    val memberId: MemberId,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberOAuth) return false

        return id == other.id &&
            externalId == other.externalId &&
            provider == other.provider &&
            memberId == other.memberId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + externalId.hashCode()
        result = 31 * result + provider.hashCode()
        result = 31 * result + memberId.hashCode()
        return result
    }

    override fun toString(): String =
        "MemberOAuth(id=$id, externalId='$externalId', provider=$provider, memberId=$memberId)"

    companion object {
        fun of(
            externalId: String,
            provider: OAuthProvider,
            memberId: MemberId,
        ): MemberOAuth =
            MemberOAuth(
                externalId = externalId,
                provider = provider,
                memberId = memberId,
            )
    }
}
