package core.entity.member

import core.domain.authorization.vo.PermissionId
import core.domain.member.aggregate.MemberPermission
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberPermissionId
import core.entity.authorization.PermissionEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "member_permissions")
class MemberPermissionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_permission_id", nullable = false, updatable = false)
    val id: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val member: MemberEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val permission: PermissionEntity,
    @Column(name = "granted_at", nullable = false, updatable = false)
    val grantedAt: Instant? = null,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
) {
    fun toDomain() =
        MemberPermission(
            id = MemberPermissionId(this.id),
            memberId = MemberId(this.member.id),
            permissionId = PermissionId(this.permission.id),
            grantedAt = this.grantedAt,
            deletedAt = this.deletedAt,
        )
}
