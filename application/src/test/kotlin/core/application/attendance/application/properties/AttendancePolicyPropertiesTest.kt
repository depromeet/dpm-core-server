package core.application.attendance.application.properties

import core.domain.attendance.vo.AttendanceTimeOffsets
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

class AttendancePolicyPropertiesTest {
    @Test
    fun `설정이 없으면 기본값 10_15_30 이다`() {
        val properties = bind(emptyMap())

        assertThat(properties.defaultOffsets).isEqualTo(AttendanceTimeOffsets(10, 15, 30))
    }

    @Test
    fun `환경 변수로 기본값을 바꿀 수 있다`() {
        val properties =
            bind(
                mapOf(
                    "attendance.policy.open-minutes-before-start" to "20",
                    "attendance.policy.late-after-start-minutes" to "5",
                    "attendance.policy.absent-after-start-minutes" to "45",
                ),
            )

        assertThat(properties.defaultOffsets).isEqualTo(AttendanceTimeOffsets(20, 5, 45))
    }

    @Test
    fun `범위를 벗어나거나 순서가 맞지 않는 기본값이면 기동에 실패한다`() {
        val invalidConfigs =
            listOf(
                mapOf("attendance.policy.open-minutes-before-start" to "-1"),
                mapOf("attendance.policy.late-after-start-minutes" to "1441"),
                mapOf("attendance.policy.absent-after-start-minutes" to "-30"),
                // 지각 시작 >= 마감
                mapOf("attendance.policy.late-after-start-minutes" to "30"),
                mapOf("attendance.policy.late-after-start-minutes" to "40"),
                // 인증 시작 == 지각 시작
                mapOf(
                    "attendance.policy.open-minutes-before-start" to "0",
                    "attendance.policy.late-after-start-minutes" to "0",
                ),
            )

        invalidConfigs.forEach { config ->
            assertThatThrownBy { bind(config) }
                .isInstanceOf(BindException::class.java)
                .rootCause()
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    private fun bind(values: Map<String, String>): AttendancePolicyProperties =
        Binder(MapConfigurationPropertySource(values))
            .bindOrCreate("attendance.policy", AttendancePolicyProperties::class.java)
}
