package core.domain.attendance.enums

enum class AttendanceGraduationStatus {
    NORMAL,
    AT_RISK,
    IMPOSSIBLE,

    /** 이전 기수 활동을 마친 멤버. 현재 기수 판정에는 쓰지 않는다. */
    COMPLETED,
}
