package core.application.attendance.application.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronExpression
import java.time.ZoneId
import java.time.ZonedDateTime

class AttendanceAutoAbsenceSchedulerTest {
    private val scheduled =
        AttendanceAutoAbsenceScheduler::class.java
            .getMethod("closeExpiredAttendances")
            .getAnnotation(Scheduled::class.java)

    @Test
    fun `매일 한국 시각 19시에 한 번 실행된다`() {
        val zone = ZoneId.of(scheduled.zone)
        val cron = CronExpression.parse(scheduled.cron)

        val beforeSeven = ZonedDateTime.parse("2026-10-01T18:59:59+09:00[Asia/Seoul]")
        val afterSeven = ZonedDateTime.parse("2026-10-01T19:00:00+09:00[Asia/Seoul]")

        assertThat(zone).isEqualTo(ZoneId.of("Asia/Seoul"))
        assertThat(cron.next(beforeSeven)).isEqualTo(ZonedDateTime.parse("2026-10-01T19:00:00+09:00[Asia/Seoul]"))
        assertThat(cron.next(afterSeven)).isEqualTo(ZonedDateTime.parse("2026-10-02T19:00:00+09:00[Asia/Seoul]"))
    }

    @Test
    fun `서버 시간대가 UTC 여도 한국 시각 19시 기준으로 실행된다`() {
        val cron = CronExpression.parse(scheduled.cron)
        // 스프링은 zone 속성 기준으로 다음 실행 시각을 계산한다. UTC 10시 = KST 19시.
        val utcMorning = ZonedDateTime.parse("2026-10-01T00:00:00Z").withZoneSameInstant(ZoneId.of(scheduled.zone))

        assertThat(cron.next(utcMorning)!!.toInstant()).isEqualTo(ZonedDateTime.parse("2026-10-01T10:00:00Z").toInstant())
        assertThat(scheduled.fixedDelay).isEqualTo(-1L)
        assertThat(scheduled.fixedRate).isEqualTo(-1L)
    }
}
