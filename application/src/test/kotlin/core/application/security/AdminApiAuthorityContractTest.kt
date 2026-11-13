package core.application.security

import core.application.attendance.presentation.controller.AttendanceQueryController
import core.application.member.presentation.controller.MemberBadgeController
import core.application.member.presentation.controller.MemberController
import core.application.member.presentation.controller.MemberManagementController
import core.application.notification.presentation.controller.NotificationController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping

/**
 * 다른 회원 정보 조회와 푸시 테스트 발송은 디퍼가 가진 권한만으로 열리지 않아야 한다.
 * 디퍼 권한: read:member, read:attendance, create:attendance, read:session 등 조회·본인 기능.
 */
class AdminApiAuthorityContractTest {
    private fun authorities(controller: Class<*>): Map<String, String> {
        val prefix =
            AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping::class.java)?.path?.firstOrNull().orEmpty()
        return controller.declaredMethods
            .mapNotNull { method ->
                val mapping =
                    AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java) ?: return@mapNotNull null
                val authority =
                    AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize::class.java)?.value ?: "(none)"
                "${mapping.method.single()} $prefix${mapping.path.firstOrNull().orEmpty()}" to authority
            }.toMap()
    }

    @Test
    fun `다른 회원 정보를 주는 운영진 조회는 관리 권한이 있어야 한다`() {
        val member = authorities(MemberController::class.java) + authorities(MemberManagementController::class.java)
        val badge = authorities(MemberBadgeController::class.java)
        val attendance = authorities(AttendanceQueryController::class.java)

        assertThat(member["GET /v3/members/overview"]).isEqualTo("hasAuthority('update:member')")
        assertThat(member["GET /v1/members/overview"]).isEqualTo("hasAuthority('update:member')")
        assertThat(member["GET /v1/members/apple/hidden-email"]).isEqualTo("hasAuthority('update:member')")
        assertThat(badge.values).containsOnly("hasAuthority('update:member')")
        assertThat(attendance["GET /v3/members/attendances"]).isEqualTo("hasAuthority('update:attendance')")
        assertThat(attendance["GET /v3/sessions/{sessionId}/attendances/{memberId}"]).isEqualTo("hasAuthority('update:attendance')")
    }

    @Test
    fun `푸시 테스트 발송은 코어 이상만 하고 디퍼 앱의 토큰 등록은 유지한다`() {
        val notification = authorities(NotificationController::class.java)

        listOf(
            "POST /v1/notifications/custom-test",
            "POST /v1/notifications/type-test",
            "GET /v1/notifications/types",
            "POST /v1/notifications/custom-test/by-tag",
            "POST /v1/notifications/type-test/by-tag",
        ).forEach { endpoint ->
            assertThat(notification[endpoint]).describedAs(endpoint).isEqualTo("hasAuthority('update:authorization')")
        }
        listOf(
            "POST /v1/notifications/tokens",
            "DELETE /v1/notifications/tokens",
            "DELETE /v1/notifications/tokens/all",
        ).forEach { endpoint ->
            assertThat(notification[endpoint]).describedAs(endpoint).isEqualTo("hasAuthority('read:member')")
        }
    }
}
