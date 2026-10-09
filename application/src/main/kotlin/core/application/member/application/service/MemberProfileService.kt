package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.AppleLoginMemberRequiredException
import core.application.member.application.exception.InvalidMemberPartException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.presentation.response.MemberProfileResponse
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberProfile
import core.domain.member.port.outbound.MemberProfilePersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
class MemberProfileService(private val profiles: MemberProfilePersistencePort) {
    @Transactional(readOnly = true)
    fun get(memberId: Long): MemberProfileResponse =
        response(available(profiles.findProfile(memberId)))

    @TrackMemberBadges
    fun complete(memberId: Long, name: String, part: String, appleOnly: Boolean = false): MemberProfileResponse {
        val normalizedName = name.trim()
        // members.name의 JPA/DB 기본 VARCHAR(255) 길이에 맞춘다. 서식 검증이며 실명 인증은 아니다.
        if (normalizedName.length !in 1..255 || !KOREAN_NAME.matches(normalizedName)) {
            throw BusinessException(MemberExceptionCode.INVALID_MEMBER_PROFILE_NAME)
        }
        val normalizedPart = runCatching { MemberPart.valueOf(part.trim().uppercase()) }
            .getOrElse { throw InvalidMemberPartException() }
        val profile = available(profiles.lockProfile(memberId))
        if (appleOnly && !profiles.hasAppleAccount(memberId)) throw AppleLoginMemberRequiredException()
        if (profile.completedAt != null) {
            if (profile.name.trim() == normalizedName && profile.part == normalizedPart) return response(profile)
            throw BusinessException(MemberExceptionCode.MEMBER_PROFILE_ALREADY_COMPLETED)
        }
        if (!profiles.completeProfile(memberId, normalizedName, normalizedPart)) {
            throw BusinessException(MemberExceptionCode.MEMBER_PROFILE_ALREADY_COMPLETED)
        }
        return MemberProfileResponse(normalizedName, normalizedPart.name, false)
    }

    private fun available(profile: MemberProfile?): MemberProfile {
        val member = profile ?: throw MemberNotFoundException()
        if (member.deletedAt != null || member.status == MemberStatus.WITHDRAWN) throw MemberDeletedException()
        return member
    }

    private fun response(profile: MemberProfile) =
        MemberProfileResponse(profile.name, profile.part?.name, profile.completedAt == null)

    private companion object {
        val KOREAN_NAME = Regex("[가-힣]+(?: +[가-힣]+)*")
    }
}
