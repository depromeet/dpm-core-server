package core.domain.authorization.aggregate

import core.domain.authorization.vo.PermissionId
import core.domain.authorization.vo.RoleId
import java.time.Instant

/**
 * @property roleId 권한이 부여된 Role의 식별자입니다.
 * @property permissionId Role에 부여된 Permission의 식별자입니다.
 * @property grantedAt 권한이 Role에 부여된 시각입니다.
 * @property revokedAt 권한이 Role에서 회수된 시각입니다.
 */
class RolePermission (
    val roleId: RoleId,
    val permissionId: PermissionId,
    val grantedAt: Instant,
    val revokedAt: Instant? = null,
) {
}
