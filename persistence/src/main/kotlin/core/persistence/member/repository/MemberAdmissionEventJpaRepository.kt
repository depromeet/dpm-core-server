package core.persistence.member.repository

import core.entity.member.MemberAdmissionEventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberAdmissionEventJpaRepository : JpaRepository<MemberAdmissionEventEntity, Long>
