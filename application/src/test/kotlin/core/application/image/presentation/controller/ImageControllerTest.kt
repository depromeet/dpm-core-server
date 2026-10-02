package core.application.image.presentation.controller

import core.application.common.exception.GlobalExceptionHandler
import core.application.image.FakeImagePersistencePort
import core.application.image.FakeImageStoragePort
import core.application.image.ImageFixtures
import core.application.image.application.service.ImageCommandService
import core.application.image.application.service.ImageQueryService
import core.application.image.application.validator.ImageValidator
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.time.Clock

/** 보안 필터와 응답 상태 aspect 없이 컨트롤러·인자 해석·예외 매핑만 확인한다. */
class ImageControllerTest {
    private val storage = FakeImageStoragePort()
    private val persistence = FakeImagePersistencePort()
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(
                ImageController(
                    ImageCommandService(ImageValidator(), storage, persistence, Clock.systemUTC()),
                    ImageQueryService(persistence, storage),
                ),
            ).setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(CurrentMemberIdArgumentResolver())
            .build()

    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    @Test
    fun `업로드 후 본인은 원본을 받고 남은 404 를 받는다`() {
        val bytes = ImageFixtures.png()
        loginAs(7L)
        mockMvc
            .perform(multipart("/v1/images").file(MockMultipartFile("file", "a.png", "image/png", bytes)))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.data.imageId").value(1))
            .andExpect(jsonPath("$.data.contentType").value("image/png"))
            .andExpect(jsonPath("$.data.size").value(bytes.size))
            .andExpect(jsonPath("$.data.objectKey").doesNotExist())

        val result =
            mockMvc
                .perform(get("/v1/images/1"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(bytes))
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn()
        val disposition = result.response.getHeader("Content-Disposition")!!
        assertThat(disposition).startsWith("inline; filename=\"").endsWith(".png\"").doesNotContain("images/")

        loginAs(8L)
        mockMvc
            .perform(get("/v1/images/1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("IMAGE-404-01"))
        assertThat(storage.calls).containsExactly("put", "get")
    }

    @Test
    fun `비로그인 요청은 401`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken("key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))

        mockMvc.perform(get("/v1/images/1")).andExpect(status().isUnauthorized)
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `file 파트가 없거나 multipart 가 아니면 400`() {
        loginAs(7L)
        mockMvc
            .perform(multipart("/v1/images").file(MockMultipartFile("other", byteArrayOf(1))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GLOBAL-400-01"))
        mockMvc
            .perform(post("/v1/images").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `지원하지 않는 형식은 415`() {
        loginAs(7L)
        mockMvc
            .perform(multipart("/v1/images").file(MockMultipartFile("file", "a.png", "image/png", ImageFixtures.gif())))
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(jsonPath("$.code").value("IMAGE-415-01"))
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `멀티파트 크기 초과는 413 으로 응답한다`() {
        val handler = GlobalExceptionHandler()
        val response = handler.handleMaxUploadSizeExceededException(MaxUploadSizeExceededException(11L * 1024 * 1024))
        val method =
            GlobalExceptionHandler::class.java.getMethod(
                "handleMaxUploadSizeExceededException",
                MaxUploadSizeExceededException::class.java,
            )

        assertThat(response.code).isEqualTo("GLOBAL-413-01")
        assertThat(AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus::class.java)!!.code)
            .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE)
    }

    private fun loginAs(memberId: Long) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(memberId.toString(), null, emptyList())
    }
}
