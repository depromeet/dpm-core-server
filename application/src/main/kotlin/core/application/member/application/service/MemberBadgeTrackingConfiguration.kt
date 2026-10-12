package core.application.member.application.service

import org.aopalliance.intercept.MethodInterceptor
import org.springframework.aop.support.DefaultPointcutAdvisor
import org.springframework.aop.support.StaticMethodMatcherPointcut
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.lang.reflect.Method

@Configuration(proxyBeanMethods = false)
class MemberBadgeTrackingConfiguration {
    @Bean
    @org.springframework.context.annotation.Role(
        org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE,
    )
    fun memberBadgeTrackingAdvisor(
        manager: ObjectProvider<PlatformTransactionManager>,
        tracker: ObjectProvider<MemberBadgeTransactionTracker>,
    ): DefaultPointcutAdvisor {
        val pointcut =
            object : StaticMethodMatcherPointcut() {
                override fun matches(
                    method: Method,
                    targetClass: Class<*>,
                ): Boolean =
                    method.isAnnotationPresent(TrackMemberBadges::class.java) ||
                        runCatching {
                            targetClass.getMethod(method.name, *method.parameterTypes)
                                .isAnnotationPresent(TrackMemberBadges::class.java)
                        }.getOrDefault(false)
            }
        return DefaultPointcutAdvisor(
            pointcut,
            MethodInterceptor { invocation ->
                TransactionTemplate(manager.getObject()).apply {
                    isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
                }.execute {
                    tracker.getObject().register()
                    invocation.proceed()
                }
            },
        )
    }
}
