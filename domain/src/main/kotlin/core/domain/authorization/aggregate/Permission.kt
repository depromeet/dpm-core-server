package core.domain.authorization.aggregate

import core.domain.authorization.enums.Action
import core.domain.authorization.enums.Resource
import core.domain.authorization.vo.PermissionId

class Permission (
    val id: PermissionId? = null,
    val action: Action,
    val resource: Resource,
) {
}
