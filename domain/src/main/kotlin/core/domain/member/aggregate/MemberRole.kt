package core.domain.member.aggregate

import core.domain.authorization.vo.RoleId
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberRoleId
import java.time.Instant

class MemberRole(
    val id: MemberRoleId? = null,
    val memberId: MemberId,
    val roleId: RoleId,
    grantedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var grantedAt: Instant? = grantedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberRole) return false

        return id == other.id &&
            memberId == other.memberId &&
            roleId == other.roleId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + memberId.hashCode()
        result = 31 * result + roleId.hashCode()
        return result
    }

    companion object {
        fun of(
            memberId: MemberId,
            roleId: RoleId,
        ): MemberRole =
            MemberRole(
                memberId = memberId,
                roleId = roleId,
                grantedAt = Instant.now(),
            )
    }
}
