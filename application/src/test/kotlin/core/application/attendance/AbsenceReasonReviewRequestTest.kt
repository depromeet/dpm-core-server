package core.application.attendance

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import core.application.attendance.presentation.request.AbsenceReasonReviewRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AbsenceReasonReviewRequestTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `승인 여부를 생략하거나 null로 보내면 반려로 처리하지 않는다`() {
        for (body in listOf("{}", """{"approved":null}""")) {
            assertThatThrownBy { mapper.readValue<AbsenceReasonReviewRequest>(body) }
                .isInstanceOf(JsonMappingException::class.java)
        }
        assertThat(mapper.readValue<AbsenceReasonReviewRequest>("""{"approved":false}""").approved).isFalse()
    }
}
