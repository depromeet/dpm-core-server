package core.application.attendance.presentation.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import core.domain.attendance.enums.AttendanceStatus

/**
 * 상태 이름 문자열만 받는다. Jackson 기본값은 순서 숫자(1, "1")도 받아 PRESENT 등으로 바꾸므로 막는다.
 * 숫자·모르는 이름은 역직렬화 예외가 되어 400 으로 거절된다.
 */
class AttendanceStatusNameDeserializer : JsonDeserializer<AttendanceStatus>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): AttendanceStatus {
        if (!p.hasToken(JsonToken.VALUE_STRING)) {
            return ctxt.handleUnexpectedToken(AttendanceStatus::class.java, p) as AttendanceStatus
        }
        val name = p.text
        return AttendanceStatus.entries.firstOrNull { it.name == name }
            ?: throw ctxt.weirdStringException(name, AttendanceStatus::class.java, "출석 상태 이름이 아닙니다")
    }
}
