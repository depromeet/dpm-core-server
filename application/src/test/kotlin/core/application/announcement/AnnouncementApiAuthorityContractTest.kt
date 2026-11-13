package core.application.announcement

import core.application.announcement.presentation.controller.AnnouncementCommandController
import core.application.announcement.presentation.controller.AnnouncementQueryController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping

/** 공지 API별 권한 표현식을 고정한다. 운영진 전용 API의 권한을 낮추면 실패한다. */
class AnnouncementApiAuthorityContractTest {
    private fun endpoints(controller: Class<*>): Set<String> {
        val prefix =
            AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping::class.java)?.path?.firstOrNull().orEmpty()
        return controller.declaredMethods
            .filterNot { it.isSynthetic || it.isBridge }
            .mapNotNull { method ->
                val mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java) ?: return@mapNotNull null
                val authority = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize::class.java)?.value ?: "(none)"
                "${mapping.method.single()} $prefix${mapping.path.firstOrNull().orEmpty()} $authority"
            }.toSet()
    }

    @Test
    fun `공지 관리와 회원 현황은 운영진 권한, 상세와 읽음 처리는 로그인, 목록은 공개다`() {
        assertThat(endpoints(AnnouncementCommandController::class.java) + endpoints(AnnouncementQueryController::class.java))
            .containsExactlyInAnyOrder(
                "POST /v1/announcements hasAuthority('update:member')",
                "PATCH /v1/announcements/{announcementId} hasAuthority('update:member')",
                "DELETE /v1/announcements/{announcementId} hasAuthority('update:member')",
                "PATCH /v1/announcements/{announcementId}/assignment-status hasAuthority('update:member')",
                "POST /v1/announcements/{announcementId}/remind-notification hasAuthority('update:member')",
                "POST /v1/announcements/{announcementId}/remind-notification-to-members hasAuthority('update:member')",
                "GET /v1/announcements/{announcementId}/mark-as-read/members hasAuthority('update:member')",
                "GET /v1/announcements/{announcementId}/assignment-status hasAuthority('update:member')",
                "POST /v1/announcements/{announcementId}/mark-as-read isAuthenticated()",
                "GET /v1/announcements/{announcementId} isAuthenticated()",
                "GET /v1/announcements permitAll()",
            )
    }
}
