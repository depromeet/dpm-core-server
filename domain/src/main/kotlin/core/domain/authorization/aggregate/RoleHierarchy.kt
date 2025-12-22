package core.domain.authorization.aggregate

import core.domain.authorization.vo.RoleId

/**
 * RoleHierarchy는 RBAC1 모델을 구현하기 위한 개념으로, 하위 Role이 상위 Role의 Permission을 암묵적으로 포함하도록 합니다.
 *
 * @property parentRoleId 상위 Role의 식별자입니다.
 * @property childRoleId 하위 Role의 식별자입니다.
 */
class RoleHierarchy(
    val parentRoleId: RoleId,
    val childRoleId: RoleId,
) {
}
