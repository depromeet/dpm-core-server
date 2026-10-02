package core.application.attendance.application.service

import core.application.image.application.dto.ImageContent
import core.application.image.application.exception.ImageNotFoundException
import core.application.image.application.service.ImageQueryService
import core.domain.absencereason.port.outbound.AbsenceReasonImagePersistencePort
import core.domain.absencereason.port.outbound.AbsenceReasonPersistencePort
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service

/**
 * 그 세션·멤버 사유서에 붙어 있고 그 멤버가 올린 이미지만 허용하며 아니면 같은 404 다.
 * DB 확인 후에만 스토리지를 부르고, 트랜잭션을 잡지 않도록 @Transactional 을 두지 않는다.
 */
@Service
class AbsenceReasonImageQueryService(
    private val absenceReasonPersistencePort: AbsenceReasonPersistencePort,
    private val absenceReasonImagePersistencePort: AbsenceReasonImagePersistencePort,
    private val imagePersistencePort: ImagePersistencePort,
    private val imageQueryService: ImageQueryService,
) {
    fun getAbsenceReasonImage(
        sessionId: SessionId,
        memberId: MemberId,
        imageId: ImageId,
    ): ImageContent {
        val absenceReasonId =
            absenceReasonPersistencePort.findBySessionIdAndMemberId(sessionId.value, memberId.value)?.id
                ?: throw ImageNotFoundException()
        if (!absenceReasonImagePersistencePort.exists(absenceReasonId.value, imageId)) throw ImageNotFoundException()

        val image =
            imagePersistencePort.findById(imageId)?.takeIf { it.isOwnedBy(memberId) }
                ?: throw ImageNotFoundException()

        return imageQueryService.loadContent(image)
    }
}
