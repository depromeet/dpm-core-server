package core.entity.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "member_merge_events",
    indexes = [Index(name = "idx_member_merge_events_retained", columnList = "retained_member_id")],
)
class MemberMergeEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_merge_event_id", nullable = false, updatable = false)
    val id: Long = 0,
    @Column(name = "actor_member_id", nullable = false, updatable = false)
    val actorMemberId: Long,
    @Column(name = "retained_member_id", nullable = false, updatable = false)
    val retainedMemberId: Long,
    @Column(name = "source_member_id", nullable = false, updatable = false)
    val sourceMemberId: Long,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
