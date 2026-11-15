package core.application.common.configuration

import core.application.common.converter.ResponseModelConverter
import io.swagger.v3.core.converter.ModelConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConvertersConfig {
    // springdoc 이 스키마를 만들기 전에 컨버터가 등록되도록 ModelConverter 빈으로 노출한다.
    // OpenApiCustomizer 안에서 addConverter 하면 이미 생성된 스키마에는 적용되지 않는다.
    @Bean
    fun responseModelConverter(): ModelConverter = ResponseModelConverter()
}
