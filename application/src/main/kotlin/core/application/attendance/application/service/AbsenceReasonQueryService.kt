package core.application.attendance.application.service

import core.application.attendance.presentation.response.MyAbsenceReasonResponse
import core.application.attendance.presentation.response.SessionAbsenceReasonItem
import core.application.attendance.presentation.response.SessionAbsenceReasonsResponse
import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.application.common.converter.TimeMapper.localDateTimeToInstant
import core.domain.absencereason.aggregate.AbsenceReason
import core.domain.absencereason.port.outbound.AbsenceReasonPersistencePort
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class AbsenceReasonQueryService(
    private val absenceReasonPersistencePort: AbsenceReasonPersistencePort,
    private val memberQueryUseCase: MemberQueryUseCase,
) {
    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }

    /**
     * 로그인한 디퍼가 해당 세션에 제출한 결석 사유서를 조회한다. 제출 이력이 없으면 null 을 반환한다.
     */
    fun getMyAbsenceReason(
        sessionId: SessionId,
        memberId: MemberId,
    ): MyAbsenceReasonResponse? =
        absenceReasonPersistencePort
            .findBySessionIdAndMemberId(sessionId.value, memberId.value)
            ?.let { reason ->
                MyAbsenceReasonResponse(
                    contents = reason.contents,
                    status = reason.status.name,
                    createdAt = instantToLocalDateTime(reason.createdAt),
                    updatedAt = instantToLocalDateTime(reason.updatedAt),
                )
            }

    /**
     * 운영진이 검토할 수 있도록 해당 세션에 제출된 모든 결석 사유서를 제출자 이름과 함께 조회한다.
     */
    fun getSessionAbsenceReasons(sessionId: SessionId): SessionAbsenceReasonsResponse {
        val reasons: List<AbsenceReason> = absenceReasonPersistencePort.findAllBySessionId(sessionId.value)
        if (reasons.isEmpty()) {
            return SessionAbsenceReasonsResponse(emptyList())
        }

        val memberNames: Map<MemberId, String> =
            memberQueryUseCase
                .getMembersByIds(reasons.map { it.memberId })
                .mapNotNull { member -> member.id?.let { it to member.name } }
                .toMap()

        val items =
            reasons
                .sortedByDescending { it.createdAt }
                .map { reason ->
                    SessionAbsenceReasonItem(
                        memberId = reason.memberId.value,
                        memberName = memberNames[reason.memberId] ?: "",
                        contents = reason.contents,
                        status = reason.status.name,
                        createdAt = instantToLocalDateTime(reason.createdAt),
                        updatedAt = instantToLocalDateTime(reason.updatedAt),
                    )
                }

        return SessionAbsenceReasonsResponse(items)
    }

}
