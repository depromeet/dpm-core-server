package core.application.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.readValue
import core.application.attendance.presentation.request.AttendanceStatusUpdateRequest
import core.domain.attendance.enums.AttendanceStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.io.ClassPathResource

class JacksonEnumNumberConfigTest {
    @Test
    fun `application yml 의 Jackson 은 enum 을 이름으로만 받고 순서 숫자는 거절한다`() {
        contextRunner().run { context ->
            val mapper = context.getBean(ObjectMapper::class.java)

            assertThat(mapper.readValue<AttendanceStatusUpdateRequest>("""{"attendanceStatus":"PRESENT"}""").attendanceStatus)
                .isEqualTo(AttendanceStatus.PRESENT)
            listOf("1", "\"1\"").forEach { number ->
                assertThatThrownBy { mapper.readValue<AttendanceStatusUpdateRequest>("""{"attendanceStatus":$number}""") }
                    .isInstanceOf(InvalidFormatException::class.java)
            }
        }
    }

    // 실제 application.yml 의 값을 그대로 넣어 설정이 빠지면 실패하게 한다.
    private fun contextRunner(): ApplicationContextRunner {
        val yaml =
            YamlPropertiesFactoryBean()
                .apply { setResources(ClassPathResource("application.yml")) }
                .`object`!!
        val key = "spring.jackson.deserialization.fail-on-numbers-for-enums"
        return ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration::class.java))
            .withPropertyValues("$key=${yaml.getProperty(key)}")
    }
}
