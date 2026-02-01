package core.domain.announcement.vo

@JvmInline
value class AnnouncementReadId(val value: Long) {
    override fun toString(): String = value.toString()
}
