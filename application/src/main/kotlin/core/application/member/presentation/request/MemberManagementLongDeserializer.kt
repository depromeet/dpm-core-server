package core.application.member.presentation.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.exc.InputCoercionException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

/** 멤버 수정 식별자는 소수나 문자열을 Long으로 자동 변환하지 않는다. */
class MemberManagementLongDeserializer : StdDeserializer<Long>(Long::class.javaObjectType) {
    override fun deserialize(
        parser: JsonParser,
        context: DeserializationContext,
    ): Long {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(handledType(), "정수 JSON 값만 입력할 수 있습니다")
        }
        return try {
            parser.longValue
        } catch (exception: InputCoercionException) {
            context.reportInputMismatch(handledType(), "Long 범위를 벗어난 값입니다")
        }
    }
}
