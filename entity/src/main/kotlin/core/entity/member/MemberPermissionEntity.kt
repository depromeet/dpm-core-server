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

/**
 * ⚠️ [2026-09] 리팩토링 시점 기준 이 테이블은 아무 곳에서도 READ 되지 않음.
 *   - 신규 코드에서 이 테이블 조회를 추가할 예정이라면 그 전에 반드시 이 주석 확인할 것.
 *   - 예외 권한 부여 기능이 실제 필요해질 때 조회 로직을 활성화하고 이 주석을 제거할 것.
 */
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
    val grantedAt: Instant,
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
