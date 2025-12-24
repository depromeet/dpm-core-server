package core.domain.member.aggregate

import core.domain.authorization.vo.PermissionId
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberPermissionId
import java.time.Instant

class MemberPermission(
    val id: MemberPermissionId? = null,
    val memberId: MemberId,
    val permissionId: PermissionId,
    grantedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var grantedAt: Instant? = grantedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberPermission) return false

        return id == other.id &&
            memberId == other.memberId &&
            permissionId == other.permissionId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + memberId.hashCode()
        result = 31 * result + permissionId.hashCode()
        return result
    }

    companion object {
        fun of(
            memberId: MemberId,
            permissionId: PermissionId,
        ): MemberPermission =
            MemberPermission(
                memberId = memberId,
                permissionId = permissionId,
                grantedAt = Instant.now(),
            )
    }
}
