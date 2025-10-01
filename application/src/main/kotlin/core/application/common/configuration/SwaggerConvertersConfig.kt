package core.application.common.configuration

import core.application.common.converter.ResponseModelConverter
import io.swagger.v3.core.converter.ModelConverters
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConvertersConfig {
    @Bean
    fun registerResponseModelConverter(): OpenApiCustomizer =
        OpenApiCustomizer { _ ->
            val converters = ModelConverters.getInstance()
            converters.addConverter(ResponseModelConverter())
        }
}
