package core.application.authorization.application.service

import core.domain.authorization.aggregate.Role
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.port.outbound.RolePersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class RoleQueryService(
    private val rolePersistencePort: RolePersistencePort,
) : RoleQueryUseCase {
    override fun getAllRoles(): List<Role> {
        return rolePersistencePort.findAll()
    }

    override fun getRolesByExternalId(externalId: String): List<String> =
        rolePersistencePort
            .findAllByMemberExternalId(externalId)
            .ifEmpty { listOf("GUEST") }

    override fun getPermissionsByMemberId(memberId: MemberId): List<String> =
        rolePersistencePort.findAllPermissionsByMemberId(memberId)

    override fun findIdByName(roleName: String): Long =
        rolePersistencePort.findIdByName(roleName)
}
