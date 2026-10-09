package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.AppleLoginMemberRequiredException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberExceptionCode
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberProfile
import core.domain.member.port.outbound.MemberProfilePersistencePort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

class MemberProfileServiceTest {
    private val profiles = mock(MemberProfilePersistencePort::class.java)
    private val service = MemberProfileService(profiles)

    @Test
    fun `완료 표시는 관리자가 파트를 지워도 입력 요구로 돌아가지 않는다`() {
        `when`(profiles.findProfile(1)).thenReturn(profile(part = null, completed = Instant.now()))
        assertThat(service.get(1).profileCompletionRequired).isFalse()
        assertThat(service.get(1).part).isNull()
    }

    @Test
    fun `최초 입력은 승인 상태와 기존 값에 관계없이 이름 파트를 함께 저장한다`() {
        MemberStatus.entries.filter { it != MemberStatus.WITHDRAWN }.forEach { status ->
            `when`(profiles.lockProfile(1)).thenReturn(profile(name = "nickname", status = status))
            `when`(profiles.completeProfile(1, "홍 길동", MemberPart.WEB)).thenReturn(true)
            val response = service.complete(1, "\t 홍 길동 \n", " web ")
            assertThat(response.name).isEqualTo("홍 길동")
            assertThat(response.part).isEqualTo("WEB")
            assertThat(response.profileCompletionRequired).isFalse()
        }
    }

    @Test
    fun `빈값 비한글 자모 중간탭과 DB 길이를 넘는 이름은 저장하지 않는다`() {
        listOf("", "  ", "Jane", "홍Gil", "ㅎㄱㄷ", "홍\t길동", "가".repeat(256)).forEach { name ->
            assertThatThrownBy { service.complete(1, name, "WEB") }
                .isInstanceOfSatisfying(BusinessException::class.java) {
                    assertThat(it.getCode()).isEqualTo(MemberExceptionCode.INVALID_MEMBER_PROFILE_NAME)
                }
        }
        listOf("", "UNASSIGNED", "INVALID").forEach { part ->
            assertThatThrownBy { service.complete(1, "홍길동", part) }.isInstanceOf(BusinessException::class.java)
        }
        verifyNoInteractions(profiles)
    }

    @Test
    fun `완성형 한글 한 글자와 255자는 허용한다`() {
        `when`(profiles.lockProfile(1)).thenReturn(profile())
        listOf("가", "가".repeat(255)).forEach { name ->
            `when`(profiles.completeProfile(1, name, MemberPart.WEB)).thenReturn(true)
            assertThat(service.complete(1, name, "WEB").name).isEqualTo(name)
        }
    }

    @Test
    fun `완료한 동일 요청은 유지하고 다른 요청은 충돌로 거절한다`() {
        `when`(profiles.lockProfile(1)).thenReturn(profile(completed = Instant.now()))
        assertThat(service.complete(1, " 홍길동 ", "WEB").profileCompletionRequired).isFalse()
        assertThatThrownBy { service.complete(1, "김길동", "WEB") }
            .isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.getCode()).isEqualTo(MemberExceptionCode.MEMBER_PROFILE_ALREADY_COMPLETED)
            }
        org.mockito.Mockito.verify(profiles, org.mockito.Mockito.never()).completeProfile(1, "홍길동", MemberPart.WEB)
    }

    @Test
    fun `구 Apple 경로도 완료 제한과 Apple 연결 검사를 공유한다`() {
        `when`(profiles.lockProfile(1)).thenReturn(profile(completed = Instant.now()))
        `when`(profiles.hasAppleAccount(1)).thenReturn(true)
        assertThatThrownBy { service.complete(1, "김길동", "SERVER", appleOnly = true) }
            .isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.getCode()).isEqualTo(MemberExceptionCode.MEMBER_PROFILE_ALREADY_COMPLETED)
            }
        `when`(profiles.hasAppleAccount(1)).thenReturn(false)
        assertThatThrownBy { service.complete(1, "홍길동", "WEB", appleOnly = true) }
            .isInstanceOf(AppleLoginMemberRequiredException::class.java)
    }

    @Test
    fun `삭제와 탈퇴 회원은 조회와 입력을 허용하지 않는다`() {
        listOf(profile(status = MemberStatus.WITHDRAWN), profile(deleted = Instant.now())).forEach {
            `when`(profiles.findProfile(1)).thenReturn(it)
            `when`(profiles.lockProfile(1)).thenReturn(it)
            assertThatThrownBy { service.get(1) }.isInstanceOf(MemberDeletedException::class.java)
            assertThatThrownBy { service.complete(1, "홍길동", "WEB") }.isInstanceOf(MemberDeletedException::class.java)
        }
    }

    private fun profile(
        name: String = "홍길동",
        part: MemberPart? = MemberPart.WEB,
        status: MemberStatus = MemberStatus.ACTIVE,
        completed: Instant? = null,
        deleted: Instant? = null,
    ) = MemberProfile(1, name, part, status, deleted, completed)
}
