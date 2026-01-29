package core.domain.announcement.vo

@JvmInline
value class AnnouncementId(val value: Long) {
    override fun toString(): String = value.toString()
}
