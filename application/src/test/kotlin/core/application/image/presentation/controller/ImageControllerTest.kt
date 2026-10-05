package core.application.image.presentation.controller

import core.application.common.exception.GlobalExceptionHandler
import core.application.image.FakeImagePersistencePort
import core.application.image.FakeImageStoragePort
import core.application.image.FakeImageUploadPersistencePort
import core.application.image.ImageFixtures
import core.application.image.application.properties.ImageStorageProperties
import core.application.image.application.service.ImageCommandService
import core.application.image.application.service.ImageQueryService
import core.application.image.application.validator.ImageValidator
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Clock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** 보안 필터와 응답 상태 aspect 없이 컨트롤러·인자 해석·예외 매핑만 확인한다. */
class ImageControllerTest {
    private val storage = FakeImageStoragePort()
    private val images = FakeImagePersistencePort()
    private val uploads = FakeImageUploadPersistencePort(images)
    private val properties = ImageStorageProperties()
    private val commandService = ImageCommandService(ImageValidator(), storage, uploads, properties, Clock.systemUTC())
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(ImageController(commandService, ImageQueryService(images, storage, properties, Clock.systemUTC())))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(CurrentMemberIdArgumentResolver())
            .build()

    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    @Test
    fun `발급, PUT, 완료, 조회 흐름과 캐시 금지 헤더`() {
        val bytes = ImageFixtures.png()
        loginAs(7L)
        val created =
            mockMvc
                .perform(createRequest("image/png", bytes.size.toLong()))
                .andExpect(status().isCreated)
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.data.uploadId").isString)
                .andExpect(jsonPath("$.data.uploadUrl").isString)
                .andExpect(jsonPath("$.data.expiresAt").exists())
                .andReturn()
        val uploadId = uploads.uploads.keys.single()
        assertThat(created.response.contentAsString).contains(uploadId)
        storage.put("uploads/$uploadId", bytes)

        mockMvc
            .perform(post("/v3/images/uploads/$uploadId/complete"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.imageId").value(1))
            .andExpect(jsonPath("$.data.contentType").value("image/png"))
            .andExpect(jsonPath("$.data.size").value(bytes.size))
            .andExpect(jsonPath("$.data.objectKey").doesNotExist())

        mockMvc
            .perform(get("/v3/images/1"))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.data.url").isString)
            .andExpect(jsonPath("$.data.expiresAt").exists())

        loginAs(8L)
        mockMvc
            .perform(get("/v3/images/1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("IMAGE-404-01"))
        mockMvc
            .perform(post("/v3/images/uploads/$uploadId/complete"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("IMAGE-404-02"))
    }

    @Test
    fun `복사 중이면 202 와 Retry-After`() {
        storage.copyCompletesImmediately = false
        loginAs(7L)
        val uploadId = createdAndPut(ImageFixtures.png())

        mockMvc
            .perform(post("/v3/images/uploads/$uploadId/complete"))
            .andExpect(status().isAccepted)
            .andExpect(header().string("Retry-After", "1"))
            .andExpect(jsonPath("$.code").value("GLOBAL-202-01"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `검증 슬롯이 차 있으면 429 와 Retry-After`() {
        loginAs(7L)
        val first = createdAndPut(ImageFixtures.png())
        val second = createdAndPut(ImageFixtures.png())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        storage.beforeDownload = {
            entered.countDown()
            release.await(5, TimeUnit.SECONDS)
        }
        val running = CompletableFuture.runAsync { commandService.completeUpload(MemberId(7L), first) }
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue()

        mockMvc
            .perform(post("/v3/images/uploads/$second/complete"))
            .andExpect(status().isTooManyRequests)
            .andExpect(header().string("Retry-After", "3"))
            .andExpect(jsonPath("$.code").value("IMAGE-429-01"))

        release.countDown()
        running.get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `지원하지 않는 형식은 415, 본문이 없으면 400`() {
        loginAs(7L)
        mockMvc
            .perform(createRequest("image/gif", 10L))
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(jsonPath("$.code").value("IMAGE-415-01"))
        mockMvc
            .perform(post("/v3/images/uploads").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest)
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `비로그인 요청은 401`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken("key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))

        mockMvc.perform(get("/v3/images/1")).andExpect(status().isUnauthorized)
        mockMvc.perform(createRequest("image/png", 10L)).andExpect(status().isUnauthorized)
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `예전 v1 이미지 경로는 남기지 않아 404`() {
        loginAs(7L)
        // 핸들러가 없을 때 예외 대신 404 를 바로 쓰게 해 전역 예외 처리(500)와 섞이지 않게 한다.
        val legacyMockMvc =
            MockMvcBuilders
                .standaloneSetup(ImageController(commandService, ImageQueryService(images, storage, properties, Clock.systemUTC())))
                .addDispatcherServletCustomizer { it.setThrowExceptionIfNoHandlerFound(false) }
                .setCustomArgumentResolvers(CurrentMemberIdArgumentResolver())
                .build()

        listOf(
            multipart("/v1/images").file(MockMultipartFile("file", "a.png", "image/png", ImageFixtures.png())),
            post("/v1/images/uploads").contentType(MediaType.APPLICATION_JSON).content("""{"contentType":"image/png","size":10}"""),
            post("/v1/images/uploads/any/complete"),
            get("/v1/images/1"),
        ).forEach { legacyMockMvc.perform(it).andExpect(status().isNotFound) }
        assertThat(storage.calls).isEmpty()
    }

    private fun createdAndPut(bytes: ByteArray): String {
        val created = commandService.createUpload(MemberId(7L), "image/png", bytes.size.toLong())
        storage.put("uploads/${created.uploadId}", bytes)
        return created.uploadId
    }

    private fun createRequest(
        contentType: String,
        size: Long,
    ) = post("/v3/images/uploads")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"contentType":"$contentType","size":$size}""")

    private fun loginAs(memberId: Long) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(memberId.toString(), null, emptyList())
    }
}
