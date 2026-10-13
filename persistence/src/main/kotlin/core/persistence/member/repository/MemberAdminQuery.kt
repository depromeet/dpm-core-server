package core.persistence.member.repository

import core.domain.authorization.vo.RoleType
import org.jooq.Field
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.ROLES
import org.jooq.impl.DSL.exists
import org.jooq.impl.DSL.selectOne

/** 조회 대상 기수의 관리자 표시. 실제 API 접근 권한은 별도로 검사한다. */
internal fun memberAdminField(memberId: Field<Long?>, cohortId: Field<Long?>): Field<Boolean> =
    exists(
        selectOne()
            .from(MEMBER_ROLES)
            .join(ROLES).on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(MEMBER_ROLES.MEMBER_ID.eq(memberId))
            .and(MEMBER_ROLES.DELETED_AT.isNull)
            .and(
                ROLES.NAME.eq(RoleType.Master.code).or(
                    ROLES.NAME.`in`(RoleType.Core.code, RoleType.Organizer.code)
                        .and(cohortId.isNotNull)
                        .and(MEMBER_ROLES.COHORT_ID.eq(cohortId).or(MEMBER_ROLES.COHORT_ID.isNull)),
                ),
            ),
    ).`as`("is_admin")
