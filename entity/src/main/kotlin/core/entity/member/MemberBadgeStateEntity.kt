package core.entity.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "member_badge_states")
@IdClass(MemberBadgeStateId::class)
class MemberBadgeStateEntity(
    @Id @Column(name = "cohort_id", nullable = false) val cohortId: Long,
    @Id @Column(name = "card", nullable = false, length = 20) val card: String,
    @Column(name = "version", nullable = false) val version: Long = 0,
    @Column(name = "acknowledged_version", nullable = false) val acknowledgedVersion: Long = 0,
    @Column(name = "target_count", nullable = false) val targetCount: Int = 0,
    @Column(name = "initialized", nullable = false) val initialized: Boolean = false,
)

data class MemberBadgeStateId(val cohortId: Long = 0, val card: String = "") : Serializable
