package core.entity.authorization

import core.domain.authorization.aggregate.RolePermission
import core.domain.authorization.vo.PermissionId
import core.domain.authorization.vo.RoleId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "role_permissions")
class RolePermissionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_permission_id", nullable = false, updatable = false)
    val id: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    val role: RoleEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    val permission: PermissionEntity,

    @Column(name = "granted_at", nullable = false)
    val grantedAt: Instant = Instant.now(),

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
) {
    fun toDomain() =
        RolePermission(
            roleId = RoleId(role.id),
            permissionId = PermissionId(permission.id),
            grantedAt = grantedAt,
            revokedAt = revokedAt,
        )
}
