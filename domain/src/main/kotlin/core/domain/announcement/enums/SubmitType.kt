package core.domain.announcement.enums

enum class SubmitType(val value: Int) {
    INDIVIDUAL(0),
    TEAM(1),
    ;

    companion object {
        fun fromValue(value: Int): SubmitType = entries.first { it.value == value }
    }
}
