package core.application.security.oauth.token

import com.fasterxml.jackson.databind.ObjectMapper
import core.domain.member.port.outbound.MemberPersistencePort
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest {
    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val members = mock(MemberPersistencePort::class.java)
    private val filter = JwtAuthenticationFilter(jwtTokenProvider, JwtTokenResolver(), ObjectMapper(), members)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `유효하지 않은 토큰 쿠키는 무시하고 비로그인으로 진행한다`() {
        `when`(jwtTokenProvider.validateToken(anyString())).thenReturn(false)
        val request = request().apply { setCookies(Cookie("accessToken", "other-env-token"), Cookie("refreshToken", "expired")) }
        val chain = MockFilterChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertThat(chain.request).isNotNull()
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `유효하지 않은 Bearer 토큰은 401로 응답한다`() {
        `when`(jwtTokenProvider.validateToken(anyString())).thenReturn(false)
        val request = request().apply { addHeader("Authorization", "Bearer invalid") }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertThat(chain.request).isNull()
        assertThat(response.status).isEqualTo(401)
        assertThat(response.contentAsString).contains("JWT-401-1")
    }

    @Test
    fun `Bearer 형식이 아닌 Authorization 헤더는 401로 응답한다`() {
        val request = request().apply { addHeader("Authorization", "Basic abc") }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertThat(chain.request).isNull()
        assertThat(response.status).isEqualTo(401)
        assertThat(response.contentAsString).contains("JWT-401-4")
    }

    @Test
    fun `유효한 토큰 쿠키는 인증한다`() {
        val authentication = UsernamePasswordAuthenticationToken("1", "valid", emptyList())
        `when`(jwtTokenProvider.validateToken("valid")).thenReturn(true)
        `when`(jwtTokenProvider.getAuthentication("valid")).thenReturn(authentication)
        `when`(jwtTokenProvider.getMemberId("valid")).thenReturn(1L)
        `when`(members.isLoginAvailable(1L)).thenReturn(true)
        val request = request().apply { setCookies(Cookie("accessToken", "valid")) }
        val chain = MockFilterChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertThat(chain.request).isNotNull()
        assertThat(SecurityContextHolder.getContext().authentication).isEqualTo(authentication)
    }

    @Test
    fun `Swagger 경로는 잘못된 Bearer 토큰이 있어도 토큰을 검사하지 않는다`() {
        val request = MockHttpServletRequest("GET", "/swagger-ui/index.html").apply { addHeader("Authorization", "Bearer invalid") }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertThat(chain.request).isNotNull()
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun `삭제 회원의 유효한 access refresh Bearer 쿠키는 기존 인증도 지우고 401로 차단한다`() {
        `when`(jwtTokenProvider.validateToken("valid")).thenReturn(true)
        `when`(jwtTokenProvider.getMemberId("valid")).thenReturn(1L)
        `when`(members.isLoginAvailable(1L)).thenReturn(false)
        listOf(
            request().apply { addHeader("Authorization", "Bearer valid") },
            request().apply { setCookies(Cookie("accessToken", "valid")) },
            request().apply { setCookies(Cookie("refreshToken", "valid")) },
        ).forEach { request ->
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("1", "valid", emptyList())
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()
            filter.doFilter(request, response, chain)
            assertThat(response.status).isEqualTo(401)
            assertThat(response.contentAsString).contains("JWT-401-1")
            assertThat(chain.request).isNull()
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
        }
    }

    private fun request() = MockHttpServletRequest("GET", "/v1/sessions")
}
