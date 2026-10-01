package core.application.common.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * 서버 기준 현재 시각. 출석 판정/자동 결석/운영진 변경 시각은 모두 이 Clock 으로 구한다.
 * 비교는 Instant 로 하므로 타임존은 결과에 영향을 주지 않는다.
 */
@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
