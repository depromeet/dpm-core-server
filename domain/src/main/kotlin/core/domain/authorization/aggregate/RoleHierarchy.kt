package core.domain.authorization.aggregate

import core.domain.authorization.vo.RoleId

class RoleHierarchy(
    val parentRoleId: RoleId,
    val childRoleId: RoleId,
) {
}
