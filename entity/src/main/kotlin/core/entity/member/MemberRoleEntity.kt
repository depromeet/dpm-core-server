package core.entity.member

import core.domain.authorization.vo.RoleId
import core.domain.member.aggregate.MemberRole
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberRoleId
import core.entity.authorization.RoleEntity
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
@Table(name = "member_roles")
class MemberRoleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_role_id", nullable = false, updatable = false)
    val id: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val member: MemberEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val role: RoleEntity,
    @Column(name = "granted_at", nullable = false, updatable = false)
    val grantedAt: Instant? = null,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
) {
    fun toDomain() =
        MemberRole(
            id = MemberRoleId(this.id),
            memberId = MemberId(this.member.id),
            roleId = RoleId(this.role.id),
            grantedAt = this.grantedAt,
            deletedAt = this.deletedAt,
        )
}
