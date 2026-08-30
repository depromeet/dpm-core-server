package core.application.common.configuration

import core.application.common.logging.MdcLoggingFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class LoggingConfig {
    /**
     * 인증 필터보다 먼저 돌아야 인증 실패 로그에도 요청 정보가 남는다.
     * [MdcLoggingFilter] 를 컴포넌트로 두지 않고 여기서만 등록해 중복 등록을 피한다.
     */
    @Bean
    fun mdcLoggingFilterRegistration(): FilterRegistrationBean<MdcLoggingFilter> =
        FilterRegistrationBean(MdcLoggingFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }
}
