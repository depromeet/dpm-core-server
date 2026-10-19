package core.application.security.annotation

import io.swagger.v3.oas.annotations.Parameter

/**
 * 현재 요청 토큰에 담긴 로그인 계정(로그인 수단 + 계정 식별자)을 주입합니다.
 * 로그인 계정 클레임이 없는 토큰(배포 전 발급분)이면 null 이 주입됩니다.
 */
@Parameter(hidden = true)
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentLoginIdentity
