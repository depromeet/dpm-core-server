package core.domain.member.port.outbound

import core.domain.member.enums.MemberAdmissionEventType

interface MemberAdmissionEventPersistencePort {
    fun record(
        memberId: Long,
        eventType: MemberAdmissionEventType,
    )
}
