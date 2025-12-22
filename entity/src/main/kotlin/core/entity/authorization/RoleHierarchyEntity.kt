package core.entity.authorization

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
@Table(name = "role_hierarchies")
class RoleHierarchyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_hierarchy_id", nullable = false, updatable = false)
    val id: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id")
    val parentRole: RoleEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_role_id")
    val childRole: RoleEntity,

    @Column(name = "granted_at", nullable = false)
    val grantedAt: Instant = Instant.now(),

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
)
