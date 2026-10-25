package core.entity.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "member_activity_events")
class MemberActivityEventEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_activity_event_id") val id: Long = 0,
    @Column(name = "member_id", nullable = false) val memberId: Long,
    @Column(name = "cohort_id", nullable = false) val cohortId: Long,
    @Column(name = "actor_member_id") val actorMemberId: Long? = null,
    @Column(name = "previous_status", length = 20) val previousStatus: String? = null,
    @Column(name = "status", nullable = false, length = 20) val status: String,
    @Column(name = "created_at", nullable = false) val createdAt: LocalDateTime,
)
