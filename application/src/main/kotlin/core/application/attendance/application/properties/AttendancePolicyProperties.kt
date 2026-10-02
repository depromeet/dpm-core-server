package core.application.attendance.application.properties

import core.domain.attendance.vo.AttendanceTimeOffsets
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 출석 시각을 모두 생략한 세션 생성에만 쓰는 기본값(분).
 * 세션에는 절대 시각으로 저장되므로 값을 바꿔도 이미 만든 세션은 바뀌지 않는다. 잘못된 값이면 기동에 실패한다.
 */
@ConfigurationProperties(prefix = "attendance.policy")
data class AttendancePolicyProperties(
    val openMinutesBeforeStart: Int = AttendanceTimeOffsets.DEFAULT_ATTENDANCE_OPEN_MINUTES_BEFORE_START,
    val lateAfterStartMinutes: Int = AttendanceTimeOffsets.DEFAULT_LATE_AFTER_START_MINUTES,
    val absentAfterStartMinutes: Int = AttendanceTimeOffsets.DEFAULT_ABSENT_AFTER_START_MINUTES,
) {
    val defaultOffsets: AttendanceTimeOffsets =
        run {
            val violation =
                AttendanceTimeOffsets.violationOf(
                    openMinutesBeforeStart,
                    lateAfterStartMinutes,
                    absentAfterStartMinutes,
                )
            require(violation == null) {
                "attendance.policy 기본값이 잘못되었습니다($violation): " +
                    "open=$openMinutesBeforeStart, late=$lateAfterStartMinutes, absent=$absentAfterStartMinutes. " +
                    "각 값은 ${AttendanceTimeOffsets.MIN_MINUTES}~${AttendanceTimeOffsets.MAX_MINUTES}분이며 " +
                    "late < absent, open + late > 0 이어야 합니다."
            }
            AttendanceTimeOffsets(openMinutesBeforeStart, lateAfterStartMinutes, absentAfterStartMinutes)
        }
}
