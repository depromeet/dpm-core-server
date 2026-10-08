package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.presentation.response.MemberBadgeCard
import core.application.member.presentation.response.MemberBadgeResponse
import core.application.member.presentation.response.MemberBadgesResponse
import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberBadgePersistencePort
import core.domain.member.port.outbound.MemberBadgeState
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class MemberBadgeService(
    private val badges: MemberBadgePersistencePort,
    private val cohorts: CohortQueryUseCase,
    private val targets: MemberManagementTargetQueryService,
) {
    /** First access initializes an acknowledged baseline; subsequent reads never change NEW. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun getBadges(): MemberBadgesResponse {
        val cohortId = cohorts.getLatestCohortId().value
        val existing = badges.findStates(cohortId)
        val states = if (existing.size == 3 && existing.all { it.initialized }) existing else initialize(cohortId)
        return MemberBadgesResponse(
            cohortId,
            MemberBadgeCard.entries.map {
                    card ->
                response(states.single { it.card == card.name })
            },
        )
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun acknowledge(
        card: MemberBadgeCard,
        cohortId: Long,
        version: Long,
    ): MemberBadgeResponse {
        if (cohorts.getLatestCohortId().value != cohortId) {
            throw BusinessException(
                MemberExceptionCode.MEMBER_BADGE_COHORT_CHANGED,
            )
        }
        val states = badges.lockStates(cohortId)
        // Recheck after waiting for a concurrent acknowledgement/writer.
        if (cohorts.getLatestCohortId().value != cohortId) {
            throw BusinessException(
                MemberExceptionCode.MEMBER_BADGE_COHORT_CHANGED,
            )
        }
        val state =
            states.single {
                it.card == card.name
            }.let { if (it.initialized) it else seed(it, snapshot(cohortId).getValue(card)) }
        if (version > state.version || version < 0) {
            throw BusinessException(
                MemberExceptionCode.INVALID_MEMBER_BADGE_VERSION,
            )
        }
        val acknowledged = state.copy(acknowledgedVersion = maxOf(state.acknowledgedVersion, version))
        badges.save(acknowledged)
        return response(acknowledged)
    }

    internal fun snapshot(cohortId: Long): Map<MemberBadgeCard, Set<Long>> {
        val members = targets.findAll(cohortId)
        return mapOf(
            MemberBadgeCard.PENDING to members.filter { it.status == MemberStatus.PENDING }.map { it.memberId }.toSet(),
            MemberBadgeCard.INCOMPLETE to members.filter { it.missingInformation }.map { it.memberId }.toSet(),
            MemberBadgeCard.AT_RISK to
                members.filter { it.graduationStatus in RISK_STATUSES }.map { it.memberId }.toSet(),
        )
    }

    internal fun isInitialized(cohortId: Long): Boolean =
        badges.findStates(cohortId).let {
            it.size == 3 && it.all { state -> state.initialized }
        }

    /** Called after business writes and flush, before the same transaction commits. */
    internal fun reconcile(
        cohortId: Long,
        baseline: Map<MemberBadgeCard, Set<Long>>?,
    ) {
        val states = badges.lockStates(cohortId)
        val current = snapshot(cohortId)
        states.forEach { original ->
            val card = MemberBadgeCard.valueOf(original.card)
            val state =
                if (original.initialized) {
                    original
                } else {
                    seed(
                        original,
                        baseline?.getValue(card) ?: current.getValue(card),
                    )
                }
            val previous = badges.findMembers(cohortId, card.name)
            val next = current.getValue(card)
            val version = Math.addExact(state.version, (next - previous).size.toLong())
            badges.replaceMembers(cohortId, card.name, previous, next)
            badges.save(
                state.copy(
                    version = version,
                    acknowledgedVersion = if (next.isEmpty()) version else state.acknowledgedVersion,
                    targetCount = next.size,
                ),
            )
        }
    }

    private fun initialize(cohortId: Long): List<MemberBadgeState> {
        val states = badges.lockStates(cohortId)
        val current = snapshot(cohortId)
        return states.map { if (it.initialized) it else seed(it, current.getValue(MemberBadgeCard.valueOf(it.card))) }
    }

    private fun seed(
        state: MemberBadgeState,
        members: Set<Long>,
    ): MemberBadgeState {
        badges.replaceMembers(state.cohortId, state.card, badges.findMembers(state.cohortId, state.card), members)
        return state.copy(targetCount = members.size, initialized = true).also(badges::save)
    }

    private fun response(state: MemberBadgeState) =
        MemberBadgeResponse(
            MemberBadgeCard.valueOf(state.card),
            state.targetCount > 0 && state.version > state.acknowledgedVersion,
            state.version,
        )

    private companion object {
        val RISK_STATUSES = setOf(AttendanceGraduationStatus.AT_RISK, AttendanceGraduationStatus.IMPOSSIBLE)
    }
}
