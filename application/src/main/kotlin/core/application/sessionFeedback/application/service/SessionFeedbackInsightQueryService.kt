package core.application.sessionFeedback.application.service

import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.application.session.application.exception.SessionNotFoundException
import core.application.sessionFeedback.application.exception.FeedbackDisabledException
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse.AspectInsightItem
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse.AspectsInsight
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse.FreeCommentsInsight
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse.ResponseSummary
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse.SatisfactionDistributionItem
import core.application.sessionFeedback.presentation.response.SessionFeedbackInsightResponse.SatisfactionInsight
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.aggregate.SessionFeedback
import core.domain.sessionFeedback.enums.SessionFeedbackAspect
import core.domain.sessionFeedback.enums.SessionFeedbackQuestion
import core.domain.sessionFeedback.enums.SessionFeedbackSatisfaction
import core.domain.sessionFeedback.port.outbound.SessionFeedbackFormPersistencePort
import core.domain.sessionFeedback.port.outbound.SessionFeedbackPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock

/**
 * `GET /v2/sessions/{sessionId}/feedbacks/insight` 응답을 조립한다.
 *
 * 마감 후 한 번에 공개하지 않고 **제출 즉시 실시간 누적** 집계한다. 수집 중에도 조회 가능하다.
 * 응답자 식별 정보(`memberId`, 이름, 팀)는 응답에 포함하지 않으며, 수집 전·응답 0건도
 * 에러가 아니라 200 + `respondentCount=0` 응답으로 내려 FE 가 빈 상태 화면을 보여주게 한다.
 */
@Service
@Transactional(readOnly = true)
class SessionFeedbackInsightQueryService(
    private val sessionPersistencePort: SessionPersistencePort,
    private val feedbackFormPersistencePort: SessionFeedbackFormPersistencePort,
    private val feedbackPersistencePort: SessionFeedbackPersistencePort,
    private val attendancePersistencePort: AttendancePersistencePort,
    private val clock: Clock,
) {
    fun getInsight(sessionId: SessionId): SessionFeedbackInsightResponse {
        val session = sessionPersistencePort.findSessionById(sessionId.value) ?: throw SessionNotFoundException()
        val form = feedbackFormPersistencePort.findBySessionId(sessionId.value) ?: throw FeedbackDisabledException()

        val feedbacks = feedbackPersistencePort.findAllBySessionId(sessionId.value)
        val targetCount = countTargets(sessionId.value)
        val now = clock.instant()

        return SessionFeedbackInsightResponse(
            sessionId = session.id?.value ?: sessionId.value,
            sessionName = session.eventName,
            status = form.statusAt(now),
            startAt = instantToLocalDateTime(form.startAt),
            endAt = instantToLocalDateTime(form.endAt),
            responseSummary = buildResponseSummary(feedbacks.size, targetCount),
            satisfaction = buildSatisfaction(feedbacks),
            likedAspects = buildAspects(feedbacks, SessionFeedbackQuestion.LIKED),
            improvementAspects = buildAspects(feedbacks, SessionFeedbackQuestion.IMPROVEMENT),
            freeComments = buildFreeComments(feedbacks),
        )
    }

    private fun countTargets(sessionId: Long): Int =
        attendancePersistencePort
            .findAllBySessionId(sessionId)
            .count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }

    private fun buildResponseSummary(
        respondentCount: Int,
        targetCount: Int,
    ): ResponseSummary =
        ResponseSummary(
            respondentCount = respondentCount,
            targetCount = targetCount,
            responseRate = ratePercent(respondentCount, targetCount),
        )

    private fun buildSatisfaction(feedbacks: List<SessionFeedback>): SatisfactionInsight {
        val respondentCount = feedbacks.size
        val average =
            if (respondentCount == 0) {
                0.0
            } else {
                BigDecimal(feedbacks.sumOf { it.satisfaction.score }.toDouble() / respondentCount)
                    .setScale(1, RoundingMode.HALF_UP)
                    .toDouble()
            }

        val countByCode = feedbacks.groupingBy { it.satisfaction }.eachCount()
        val distribution =
            SessionFeedbackSatisfaction.entries
                .sortedByDescending { it.score }
                .map { code ->
                    val count = countByCode[code] ?: 0
                    SatisfactionDistributionItem(
                        code = code,
                        label = SessionFeedbackInsightResponse.SATISFACTION_INSIGHT_LABELS.getValue(code),
                        count = count,
                        rate = ratePercent(count, respondentCount),
                    )
                }

        return SatisfactionInsight(
            average = average,
            maxScore = SessionFeedbackInsightResponse.MAX_SCORE,
            distribution = distribution,
        )
    }

    private fun buildAspects(
        feedbacks: List<SessionFeedback>,
        question: SessionFeedbackQuestion,
    ): AspectsInsight {
        val perRespondent = feedbacks.map { it.aspectsOf(question) }
        val respondentCount = perRespondent.count { it.isNotEmpty() }
        val totalSelectionCount = perRespondent.sumOf { it.size }

        val countByAspect: Map<SessionFeedbackAspect, Int> =
            perRespondent
                .flatten()
                .groupingBy { it }
                .eachCount()

        val items =
            countByAspect.entries
                .asSequence()
                .filterNot { (code, count) -> code == SessionFeedbackAspect.ETC && count == 0 }
                .map { (code, count) ->
                    AspectInsightItem(
                        code = code,
                        label = SessionFeedbackInsightResponse.ASPECT_LABELS.getValue(code),
                        count = count,
                        rate = ratePercent(count, respondentCount),
                    )
                }.sortedWith(compareByDescending<AspectInsightItem> { it.count }.thenBy { it.code.ordinal })
                .toList()

        val etcComments =
            feedbacks
                .asSequence()
                .filter { question in it.questionsWithEtc() }
                .mapNotNull { it.etcTextOf(question) }
                .filter { it.isNotBlank() }
                .toList()

        return AspectsInsight(
            totalSelectionCount = totalSelectionCount,
            items = items,
            etcComments = etcComments,
        )
    }

    private fun buildFreeComments(feedbacks: List<SessionFeedback>): FreeCommentsInsight {
        val items =
            feedbacks
                .asSequence()
                .sortedBy { it.submittedAt }
                .mapNotNull { it.freeComment }
                .filter { it.isNotBlank() }
                .toList()
        return FreeCommentsInsight(count = items.size, items = items)
    }

    private fun ratePercent(
        numerator: Int,
        denominator: Int,
    ): Int = if (denominator == 0) 0 else (numerator * 100) / denominator

    private fun SessionFeedback.questionsWithEtc(): Set<SessionFeedbackQuestion> =
        buildSet {
            if (SessionFeedbackAspect.ETC in likedAspects) add(SessionFeedbackQuestion.LIKED)
            if (SessionFeedbackAspect.ETC in improvementAspects) add(SessionFeedbackQuestion.IMPROVEMENT)
        }

    private fun SessionFeedback.etcTextOf(question: SessionFeedbackQuestion): String? =
        when (question) {
            SessionFeedbackQuestion.LIKED -> likedEtc
            SessionFeedbackQuestion.IMPROVEMENT -> improvementEtc
        }
}
