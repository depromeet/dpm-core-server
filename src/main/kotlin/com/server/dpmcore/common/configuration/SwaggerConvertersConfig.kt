package com.server.dpmcore.common.configuration

import com.server.dpmcore.common.converter.ResponseModelConverter
import io.swagger.v3.core.converter.ModelConverters
import org.slf4j.LoggerFactory
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConvertersConfig {

    @Bean
    fun registerResponseModelConverter(): OpenApiCustomizer = OpenApiCustomizer { _ ->
        val converters = ModelConverters.getInstance()
        converters.addConverter(ResponseModelConverter())
            
    }
}
