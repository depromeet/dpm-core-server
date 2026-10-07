package core.application.image.presentation.controller

import core.application.image.presentation.response.ImageUrlResponse
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

/**
 * 다운로드 URL 로 보낸다(302). 바이트는 서버를 거치지 않고 저장소에서 바로 내려간다.
 * Location 이 곧 권한이라 캐시하지 않는다.
 */
fun redirectToDownload(download: ImageUrlResponse): ResponseEntity<Void> =
    ResponseEntity
        .status(HttpStatus.FOUND)
        .location(URI.create(download.url))
        .cacheControl(CacheControl.noStore().cachePrivate())
        .build()
