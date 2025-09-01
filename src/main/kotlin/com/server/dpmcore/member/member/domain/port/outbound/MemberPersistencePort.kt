package com.server.dpmcore.member.member.domain.port.outbound

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.model.query.MemberNameAuthorityQueryModel

interface MemberPersistencePort {
    fun save(member: Member): Member

    fun findByEmail(email: String): Member?

    fun findById(memberId: Long): Member?

    fun existsById(memberId: Long): Boolean

    fun existsDeletedMemberById(memberId: Long): Boolean

    fun findAllMemberIdByAuthorityIds(authorityIds: List<AuthorityId>): List<MemberId>

    fun findAllByCohort(value: String): List<MemberId>

    fun findMemberNameAndAuthorityByMemberId(memberId: MemberId): List<MemberNameAuthorityQueryModel>

    fun findMemberTeamByMemberId(memberId: MemberId): Int?
}
