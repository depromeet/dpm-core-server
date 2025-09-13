package com.server.dpmcore.attendance.domain.model

/**
 * 영속화 하지 않는 출석 상태를 결과로 반환하기 위해 사용합니다.
 *
 * 출석 체크 시간 보다 일찍 출석 체크를 시도한 경우 디프만 출석 정책 상 해당 상태를 저장하지 않으므로 TooEarly 를 반환합니다.
 *
 * @author LeeHanEum
 * @since 2025.09.13
 */
sealed class AttendanceCheck {
    data class Success(val status: AttendanceStatus) : AttendanceCheck()

    data object TooEarly : AttendanceCheck()
}
