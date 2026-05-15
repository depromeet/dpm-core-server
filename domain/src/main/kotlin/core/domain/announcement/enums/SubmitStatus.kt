package core.domain.announcement.enums

enum class SubmitStatus(
    val value: Int,
) {
    PENDING(0),
    SUBMITTED(1),
    LATE_SUBMITTED(2),
    NOT_SUBMITTED(3),
    ;

    companion object {
        fun fromValue(value: Int): SubmitStatus = entries.first { it.value == value }
    }

    fun sortedValue(): Int =
        when (this) {
            SUBMITTED -> 0
            PENDING -> 1
            LATE_SUBMITTED -> 2
            NOT_SUBMITTED -> 3
        }
}
