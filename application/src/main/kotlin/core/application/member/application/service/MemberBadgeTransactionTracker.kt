package core.application.member.application.service

import core.application.cohort.application.service.CohortQueryService
import jakarta.persistence.EntityManager
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class MemberBadgeTransactionTracker(
    private val badges: MemberBadgeService,
    private val cohorts: CohortQueryService,
    private val entityManager: EntityManager,
) {
    fun register() {
        check(TransactionSynchronizationManager.isActualTransactionActive()) { "Badge tracking requires a transaction" }
        check(!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            "Badge tracking requires a write transaction"
        }
        check(
            TransactionSynchronizationManager.getCurrentTransactionIsolationLevel() ==
                TransactionDefinition.ISOLATION_READ_COMMITTED,
        ) {
            "Badge writes must start in READ_COMMITTED, including the outer caller"
        }
        if (TransactionSynchronizationManager.getSynchronizations().any { it is BadgeSynchronization }) return
        val initialCohort = currentCohortOrNull()
        // Only the first write needs the pre-write baseline. Do not hold badge locks during business/provider work.
        val baseline = initialCohort?.takeUnless(badges::isInitialized)?.let(badges::snapshot)
        TransactionSynchronizationManager.registerSynchronization(
            BadgeSynchronization {
                entityManager.flush()
                currentCohortOrNull()?.let { cohortId ->
                    badges.reconcile(cohortId, baseline.takeIf { initialCohort == cohortId })
                }
            },
        )
    }

    private fun currentCohortOrNull(): Long? = cohorts.findCurrentCohortOrNull()?.id?.value

    private class BadgeSynchronization(private val reconcile: () -> Unit) : TransactionSynchronization {
        override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

        override fun beforeCommit(readOnly: Boolean) = reconcile()
    }
}
