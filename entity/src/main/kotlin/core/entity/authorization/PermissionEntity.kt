package core.entity.authorization

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "permissions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_permission_resource_action",
            columnNames = ["resource", "action"]
        )
    ]
)
class PermissionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id", nullable = false, updatable = false)
    val id: Long,

    @Column(nullable = false)
    val resource: String,

    @Column(nullable = false)
    val action: String,
)
