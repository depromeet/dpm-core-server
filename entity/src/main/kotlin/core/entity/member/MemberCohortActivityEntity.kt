package core.entity.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(name = "member_cohort_activities", uniqueConstraints = [UniqueConstraint(name = "uk_member_cohort_activity", columnNames = ["member_id", "cohort_id"])])
class MemberCohortActivityEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_cohort_activity_id") val id: Long = 0,
    @Column(name = "member_id", nullable = false) val memberId: Long,
    @Column(name = "cohort_id", nullable = false) val cohortId: Long,
    @Column(nullable = false, length = 20) val status: String,
    @Column(name = "suspended_at") val suspendedAt: LocalDateTime? = null,
    @Column(name = "created_at", nullable = false) val createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false) val updatedAt: LocalDateTime,
)
