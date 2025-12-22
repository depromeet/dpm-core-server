package core.domain.authorization.aggregate

import core.domain.authorization.vo.PermissionId
import core.domain.authorization.vo.RoleId
import java.time.Instant

class RolePermission (
    val roleId: RoleId,
    val permissionId: PermissionId,
    val grantedAt: Instant,
    val revokedAt: Instant? = null,
) {
}
