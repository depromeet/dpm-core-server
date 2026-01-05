package core.domain.authorization.aggregate

import core.domain.authorization.enums.Action
import core.domain.authorization.enums.Resource
import core.domain.authorization.vo.PermissionId

/**
 * @property action 권한이 허용하는 동작을 나타냅니다. (예: CREATE, READ, UPDATE, DELETE)
 * @property resource 권한이 적용되는 리소스를 나타냅니다. (예: ATTENDANCE, SESSION, MEMBER)
 */
class Permission(
    val id: PermissionId? = null,
    val action: Action,
    val resource: Resource,
)
