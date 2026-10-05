package core.application.attendance.presentation.mapper

import core.application.attendance.presentation.response.MemberDetailAbsenceReasonImageInfo
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import core.domain.attendance.port.outbound.query.MemberAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionRosterQueryModel
import core.domain.team.vo.TeamNumber
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

class AttendanceMapperTest {
    @Test
    fun `세션별 출석은 출석 인증 시각과 장소를 주고, 온라인 세션의 장소는 온라인이다`() {
        val sessions =
            listOf(
                session(sessionId = 1, isOnline = false, place = "공덕 창업허브", attendedAt = Instant.parse("2026-09-05T04:58:21Z")),
                session(sessionId = 2, isOnline = true, place = "", attendedAt = null),
            )

        val response = AttendanceMapper.toDetailMemberAttendancesResponse(member(), sessions, "NORMAL")

        val offline = response.sessions[0]
        assertThat(offline.week).isEqualTo(1)
        assertThat(offline.eventName).isEqualTo("1주차 세션")
        assertThat(offline.date).isEqualTo(LocalDateTime.parse("2026-09-05T14:00:00"))
        assertThat(offline.attendedAt).isEqualTo(LocalDateTime.parse("2026-09-05T13:58:21"))
        assertThat(offline.place).isEqualTo("공덕 창업허브")
        val online = response.sessions[1]
        assertThat(online.attendedAt).isNull()
        assertThat(online.place).isEqualTo("온라인")
    }

    @Test
    fun `결석 사유서 첨부는 id 와 파일명을 표시 순서대로 주고 imageIds 도 같은 순서로 유지한다`() {
        val reason =
            MemberSessionAttendanceQueryModel.AbsenceReason(
                id = 3,
                contents = "병원 진료",
                status = "PENDING",
                images =
                    listOf(
                        MemberSessionAttendanceQueryModel.Image(imageId = 15, fileName = "진단서.jpg"),
                        MemberSessionAttendanceQueryModel.Image(imageId = 12, fileName = null),
                    ),
            )

        val response =
            AttendanceMapper.toDetailMemberAttendancesResponse(member(), listOf(session(1, false, "공덕", null, reason)), "NORMAL")

        val absenceReason = response.sessions.single().absenceReason!!
        assertThat(absenceReason.imageIds).containsExactly(15L, 12L)
        assertThat(absenceReason.images).containsExactly(
            MemberDetailAbsenceReasonImageInfo(imageId = 15, fileName = "진단서.jpg"),
            MemberDetailAbsenceReasonImageInfo(imageId = 12, fileName = null),
        )
    }

    @Test
    fun `세션 명단은 운영진이 바꾼 기록(인증 시각이 남은 예전 기록 포함)의 인증 시각을 숨기고 팀 없음과 결석 사유를 그대로 준다`() {
        val attendedAt = Instant.parse("2026-09-05T04:58:21Z")
        val roster =
            listOf(
                rosterRow(memberId = 1, teamNumber = 1, status = "PRESENT", attendedAt = attendedAt, updatedAt = null),
                rosterRow(memberId = 2, teamNumber = 1, status = "ABSENT", attendedAt = attendedAt, updatedAt = Instant.parse("2026-09-05T06:00:00Z")),
                rosterRow(memberId = 3, teamNumber = null, status = "EXCUSED_ABSENT", attendedAt = null, updatedAt = null, absenceReason = "병원 진료"),
            )

        val response = AttendanceMapper.toSessionRosterResponse(roster, myTeamNumber = null)

        // 순서는 조회 결과 그대로다
        assertThat(response.members.map { it.id }).containsExactly(1L, 2L, 3L)
        assertThat(response.members.map { it.attendedAt }).containsExactly(LocalDateTime.parse("2026-09-05T13:58:21"), null, null)
        assertThat(response.members.map { it.isManuallyUpdated }).containsExactly(false, true, false)
        assertThat(response.members.map { it.teamNumber }).containsExactly(1, 1, null)
        assertThat(response.members.map { it.absenceReason }).containsExactly(null, null, "병원 진료")
        assertThat(response.members.map { it.attendanceStatus }).containsExactly("PRESENT", "ABSENT", "EXCUSED_ABSENT")
        assertThat(response.myTeamNumber).isNull()
        assertThat(response.totalElements).isEqualTo(3)
        assertThat(AttendanceMapper.toSessionRosterResponse(emptyList(), myTeamNumber = 4).myTeamNumber).isEqualTo(4)
    }

    @Test
    fun `사람별 목록은 조회 순서와 팀 없음(0)을 그대로 두고 판정, 내 팀, 전체 수만 붙인다`() {
        val members =
            listOf(
                AttendanceMapper.toMemberAttendanceResponse(
                    MemberAttendanceQueryModel(1, "신민철", TeamNumber(2), false, "SERVER", AttendanceSummaryQueryModel(4, 2, 0, 0, 0, 0)),
                    evaluation = "AT_RISK",
                ),
                AttendanceMapper.toMemberAttendanceResponse(
                    MemberAttendanceQueryModel(2, "이정호", TeamNumber(0), true, null, AttendanceSummaryQueryModel(4, 0, 0, 0, 0, 0)),
                    evaluation = "NORMAL",
                ),
            )

        // 전체 수는 목록 크기가 아니라 받은 값 그대로다
        val response = AttendanceMapper.toMemberAttendancesResponse(members, myTeamNumber = null, totalElements = 5)

        assertThat(response.members.map { it.id }).containsExactly(1L, 2L)
        assertThat(response.members.map { it.teamNumber }).containsExactly(TeamNumber(2), TeamNumber(0))
        assertThat(response.members.map { it.part }).containsExactly("SERVER", null)
        assertThat(response.members.map { it.isAdmin }).containsExactly(false, true)
        assertThat(response.members.map { it.attendanceStatus }).containsExactly("AT_RISK", "NORMAL")
        assertThat(response.myTeamNumber).isNull()
        assertThat(response.totalElements).isEqualTo(5)
        assertThat(
            AttendanceMapper.toMemberAttendancesResponse(emptyList(), myTeamNumber = 3, totalElements = 0).myTeamNumber,
        ).isEqualTo(3)
    }

    private fun rosterRow(
        memberId: Long,
        teamNumber: Int?,
        status: String,
        attendedAt: Instant?,
        updatedAt: Instant?,
        absenceReason: String? = null,
    ) = SessionRosterQueryModel(
        memberId = memberId,
        name = "멤버$memberId",
        teamNumber = teamNumber,
        isAdmin = false,
        part = "SERVER",
        attendanceStatus = status,
        attendedAt = attendedAt,
        updatedAt = updatedAt,
        absenceReason = absenceReason,
    )

    private fun member() =
        MemberDetailAttendanceQueryModel(
            memberId = 1,
            memberName = "신민규",
            teamNumber = TeamNumber(1),
            isAdmin = false,
            part = "SERVER",
            summary = AttendanceSummaryQueryModel(2, 1, 0, 0, 0, 0),
        )

    private fun session(
        sessionId: Long,
        isOnline: Boolean,
        place: String,
        attendedAt: Instant?,
        absenceReason: MemberSessionAttendanceQueryModel.AbsenceReason? = null,
    ) = MemberSessionAttendanceQueryModel(
        sessionId = sessionId,
        sessionWeek = sessionId.toInt(),
        sessionEventName = "${sessionId}주차 세션",
        sessionDate = Instant.parse("2026-09-05T05:00:00Z"),
        sessionIsOnline = isOnline,
        sessionPlace = place,
        sessionAttendanceStatus = "PRESENT",
        attendedAt = attendedAt,
        absenceReason = absenceReason,
    )
}
