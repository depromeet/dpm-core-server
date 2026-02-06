package core.domain.announcement.enums

enum class AnnouncementType(val value: Int) {
    GENERAL(0),
    ASSIGNMENT(1),
    ;

    companion object {
        fun fromValue(value: Int): AnnouncementType = entries.first { it.value == value }
    }
}
