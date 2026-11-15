package core.application.common.configuration

import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OperationIdCustomizer {
    // Kotlin value class(MemberId 등) 파라미터는 JVM 이름이 맹글링되어 operationId 에 -KYnTANU 같은 해시 접미사가 붙는다.
    // 시그니처가 바뀌면 해시도 바뀌어 클라이언트에서 `operations["..."]` 키가 깨지므로 접미사를 제거한다.
    // 접미사 제거로 이름이 겹치면 springdoc 이 _1 을 붙여 유지한다. 겹치는 경우만 @Operation(operationId = ...)로 지정한다.
    @Bean
    fun stripManglingSuffixFromOperationId(): OperationCustomizer =
        OperationCustomizer { operation, _ ->
            val id = operation.operationId
            if (!id.isNullOrBlank()) {
                operation.operationId = MANGLING_SUFFIX.replace(id, "")
            }
            operation
        }

    companion object {
        private val MANGLING_SUFFIX = Regex("-[A-Za-z0-9_]+$")
    }
}
