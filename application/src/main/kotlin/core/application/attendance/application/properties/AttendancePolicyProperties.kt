package core.application.attendance.application.properties

import core.domain.attendance.vo.AttendanceTimeOffsets
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 세션 생성 시 출석 시각 세 개를 모두 생략했을 때만 쓰는 기본 출석 시간(분). 환경 변수로 설정한다.
 *
 * 생성 시점에 절대 시각으로 세션에 저장되므로 값을 바꿔도 이미 만든 세션(과거/현재 기수, 아직 열리지 않은 세션 포함)과
 * 출석 기록은 바뀌지 않는다. 범위(0~1440)와 순서(인증 시작 < 지각 시작 < 마감)를 기동 시 검증하며,
 * 어긋나면 서버가 기동되지 않는다.
 */
@ConfigurationProperties(prefix = "attendance.policy")
data class AttendancePolicyProperties(
    val openMinutesBeforeStart: Int = AttendanceTimeOffsets.DEFAULT_ATTENDANCE_OPEN_MINUTES_BEFORE_START,
    val lateAfterStartMinutes: Int = AttendanceTimeOffsets.DEFAULT_LATE_AFTER_START_MINUTES,
    val absentAfterStartMinutes: Int = AttendanceTimeOffsets.DEFAULT_ABSENT_AFTER_START_MINUTES,
) {
    /** 기동 시 검증된 기본 오프셋. 잘못된 값이면 바인딩 단계에서 예외가 나 서버가 기동되지 않는다. */
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
