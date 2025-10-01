package core.application.common.configuration

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderNameCase
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class JooqDslConfig {
    @Bean
    fun dslContext(dataSource: DataSource): DSLContext {
        val settings =
            Settings()
                .withRenderNameCase(RenderNameCase.LOWER)
                .withRenderQuotedNames(org.jooq.conf.RenderQuotedNames.NEVER)

        return DSL.using(dataSource, SQLDialect.MYSQL, settings)
    }
}
