package core.application.member.application.service

import core.domain.member.event.MemberActivatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class MemberActivatedEventListener(
    private val initializer: MemberActivationInitializer,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleMemberActivatedEvent(memberActivatedEvent: MemberActivatedEvent) {
        initializer.initialize(memberActivatedEvent.memberId, memberActivatedEvent.cohortId)
    }
}
