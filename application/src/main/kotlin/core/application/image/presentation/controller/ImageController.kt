package core.application.image.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.image.application.service.ImageCommandService
import core.application.image.application.service.ImageQueryService
import core.application.image.presentation.mapper.ImageResponseMapper
import core.application.image.presentation.response.ImageUploadResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

// SecurityConfig 가 /v1/** 를 permitAll 하므로 인증은 메서드에서 강제한다.
@RestController
class ImageController(
    private val imageCommandService: ImageCommandService,
    private val imageQueryService: ImageQueryService,
) : ImageApi {
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/v1/images")
    override fun uploadImage(
        @CurrentMemberId memberId: MemberId,
        @RequestPart("file") file: MultipartFile,
    ): CustomResponse<ImageUploadResponse> = CustomResponse.created(imageCommandService.upload(memberId, file))

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/v1/images/{imageId}")
    override fun getImage(
        @CurrentMemberId memberId: MemberId,
        @PathVariable imageId: ImageId,
    ): ResponseEntity<ByteArray> = ImageResponseMapper.toInlineResponse(imageQueryService.getImage(memberId, imageId))
}
