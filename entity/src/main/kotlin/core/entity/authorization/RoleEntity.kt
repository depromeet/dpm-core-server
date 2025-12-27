package core.entity.authorization

import core.domain.authorization.aggregate.Role
import core.domain.authorization.vo.RoleId
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "roles",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_role_name", columnNames = ["name"]),
    ],
)
class RoleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false, updatable = false)
    val id: Long,

    @Column(nullable = false)
    val name: String,

    @OneToMany(
        mappedBy = "role",
        cascade = [CascadeType.PERSIST, CascadeType.REMOVE],
        orphanRemoval = true,
    )
    private val rolePermissions: MutableSet<RolePermissionEntity> = mutableSetOf(),

) {
    fun toDomain(): Role =
        Role(
            id = RoleId(id),
            name = name,
            rolePermissions = rolePermissions.map { it.toDomain() }.toMutableSet()
        )

}
