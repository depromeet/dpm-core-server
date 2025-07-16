package com.server.dpmcore.member.member.application

import com.server.dpmcore.member.member.application.exception.MemberNotFoundException
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.port.outbound.MemberPersistencePort
import com.server.dpmcore.member.member.presentation.response.MemberDetailsResponse
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberPersistencePort: MemberPersistencePort,
) {
    fun memberMe(memberId: MemberId): MemberDetailsResponse =
        MemberDetailsResponse.from(
            getMemberById(memberId),
        )

    fun getMemberById(memberId: MemberId) =
        memberPersistencePort.findById(memberId.value)
            ?: throw MemberNotFoundException()
}
