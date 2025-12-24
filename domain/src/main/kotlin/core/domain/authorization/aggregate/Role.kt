package core.domain.authorization.aggregate

import core.domain.authorization.vo.RoleId

/**
 * 시스템 내에서 멤버에게 부여되는 역할(Role) 을 나타내는 도메인 객체 입니다.
 *
 * @property name 역할의 고유한 이름 입니다. (ex: "17기 운영진", "코어" 등)
 */
class Role (
    val id: RoleId? = null,
    val name: String,
    rolePermissions: MutableSet<RolePermission> = mutableSetOf(),
) {
    private val _rolePermissions: MutableSet<RolePermission> = rolePermissions

    val rolePermissions: Set<RolePermission>
        get() = _rolePermissions.toSet()

    fun addRolePermission(rolePermission: RolePermission) {
        _rolePermissions.add(rolePermission)
    }
}
