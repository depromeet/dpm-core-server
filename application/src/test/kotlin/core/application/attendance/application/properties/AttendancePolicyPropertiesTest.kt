package core.application.attendance.application.properties

import core.domain.attendance.vo.AttendanceTimeOffsets
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

/**
 * application.yml 의 attendance.policy 설정이 기동 시 어떻게 바인딩/검증되는지 실제 스프링 Binder 로 확인한다.
 */
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
    fun `범위를 벗어난 기본값이면 기동에 실패한다`() {
        listOf(
            "attendance.policy.open-minutes-before-start" to "-1",
            "attendance.policy.late-after-start-minutes" to "1441",
            "attendance.policy.absent-after-start-minutes" to "-30",
        ).forEach { entry ->
            assertThatThrownBy { bind(mapOf(entry)) }
                .isInstanceOf(BindException::class.java)
                .rootCause()
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `지각 시작이 마감 이상이거나 인증 시작과 지각 시작이 같으면 기동에 실패한다`() {
        listOf(
            mapOf("attendance.policy.late-after-start-minutes" to "30"),
            mapOf("attendance.policy.late-after-start-minutes" to "40"),
            mapOf(
                "attendance.policy.open-minutes-before-start" to "0",
                "attendance.policy.late-after-start-minutes" to "0",
            ),
        ).forEach { config ->
            assertThatThrownBy { bind(config) }
                .isInstanceOf(BindException::class.java)
                .rootCause()
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `직접 생성할 때도 잘못된 기본값은 거절한다`() {
        assertThatThrownBy { AttendancePolicyProperties(10, 30, 30) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun bind(values: Map<String, String>): AttendancePolicyProperties =
        Binder(MapConfigurationPropertySource(values))
            .bindOrCreate("attendance.policy", AttendancePolicyProperties::class.java)
}
