package core.entity.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "member_badge_memberships")
@IdClass(MemberBadgeMembershipId::class)
class MemberBadgeMembershipEntity(
    @Id @Column(name = "cohort_id", nullable = false) val cohortId: Long,
    @Id @Column(name = "card", nullable = false, length = 20) val card: String,
    @Id @Column(name = "member_id", nullable = false) val memberId: Long,
)

data class MemberBadgeMembershipId(val cohortId: Long = 0, val card: String = "", val memberId: Long = 0) : Serializable
