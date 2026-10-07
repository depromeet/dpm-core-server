package core.domain.session.enums

/**
 * 세션의 출석 인증 진행 상태. 저장하지 않고 조회 시각으로 계산합니다.
 *
 * @property NOT_STARTED 출석 시작 시각 전
 * @property IN_PROGRESS 출석 시작 시각부터 인증 마감(결석 시작) 시각 전까지. 지각 구간을 포함합니다.
 * @property CLOSED 인증 마감 시각 정각부터
 */
enum class SessionAttendanceStatus {
    NOT_STARTED,
    IN_PROGRESS,
    CLOSED,
}
