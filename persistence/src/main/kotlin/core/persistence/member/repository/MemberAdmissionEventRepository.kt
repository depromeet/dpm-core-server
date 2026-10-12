package core.persistence.member.repository

import core.domain.member.enums.MemberAdmissionEventType
import core.domain.member.port.outbound.MemberAdmissionEventPersistencePort
import core.entity.member.MemberAdmissionEventEntity
import org.springframework.stereotype.Repository

@Repository
class MemberAdmissionEventRepository(
    private val repository: MemberAdmissionEventJpaRepository,
) : MemberAdmissionEventPersistencePort {
    override fun record(
        memberId: Long,
        eventType: MemberAdmissionEventType,
    ) {
        repository.save(MemberAdmissionEventEntity(memberId = memberId, eventType = eventType.name))
    }
}
