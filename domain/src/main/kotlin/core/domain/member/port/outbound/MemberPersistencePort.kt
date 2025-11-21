package core.domain.member.port.outbound

import core.domain.authority.vo.AuthorityId
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.query.MemberNameAuthorityQueryModel
import core.domain.member.vo.MemberId

interface MemberPersistencePort {
    fun save(member: Member): Member

    fun findByEmail(email: String): Member?

    fun findById(memberId: Long): Member?

    fun findAllByIds(ids: List<MemberId>): List<Member>

    fun existsById(memberId: Long): Boolean

    fun existsDeletedMemberById(memberId: Long): Boolean

    fun findAllMemberIdByAuthorityIds(authorityIds: List<AuthorityId>): List<MemberId>

    fun findAllByCohort(value: String): List<MemberId>

    fun findMemberNameAndAuthorityByMemberId(memberId: MemberId): List<MemberNameAuthorityQueryModel>

    fun findMemberTeamByMemberId(memberId: MemberId): Int?
}
