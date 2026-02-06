package core.domain.announcement.vo

@JvmInline
value class AnnouncementAssignmentId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
