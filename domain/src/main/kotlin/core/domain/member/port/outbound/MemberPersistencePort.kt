package core.domain.member.port.outbound

import core.domain.authorization.vo.RoleId
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.query.MemberNameRoleQueryModel
import core.domain.member.vo.MemberId

interface MemberPersistencePort {
    fun save(member: Member): Member

    fun findByEmail(email: String): Member?

    fun findById(memberId: Long): Member?

    fun findAllByIds(ids: List<MemberId>): List<Member>

    fun existsById(memberId: Long): Boolean

    fun existsDeletedMemberById(memberId: Long): Boolean

    fun findByNameAndSignupEmail(name: String, signupEmail: String): Member?

    fun findAllMemberIdByRoleIds(roleIds: List<RoleId>): List<MemberId>

    fun findAllByCohort(value: String): List<MemberId>

    fun findMemberNameAndRoleByMemberId(memberId: MemberId): List<MemberNameRoleQueryModel>

    fun findMemberTeamByMemberId(memberId: MemberId): Int?
}
