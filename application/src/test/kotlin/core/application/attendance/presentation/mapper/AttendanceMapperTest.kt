package core.application.attendance.presentation.mapper

import core.application.attendance.presentation.response.MemberDetailAbsenceReasonImageInfo
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
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
