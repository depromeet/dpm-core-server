package com.server.dpmcore.member.member.domain.port.inbound

interface HandleMemberLogoutUseCase {
    fun handleLogoutSuccess(memberId: Long): Boolean
}
