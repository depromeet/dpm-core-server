package com.server.dpmcore.member.member.application

import org.springframework.stereotype.Service

@Service
class MemberLogoutService : HandleMemberLogoutUseCase {
    override fun handleLogoutSuccess(memberId: Long): Boolean {
        // Implement the logic to handle member logout
        // For example, invalidate the session or token associated with the member
        // Return true if logout was successful, false otherwise
        return true
    }
}
