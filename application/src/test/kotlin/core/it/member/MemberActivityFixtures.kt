package core.it.member

import org.springframework.jdbc.core.JdbcTemplate

internal object MemberActivityFixtures {
    fun clear(jdbc: JdbcTemplate) {
        jdbc.execute("delete from member_activity_events")
        jdbc.execute("delete from member_cohort_activities")
    }

    fun seed(jdbc: JdbcTemplate) {
        jdbc.execute(
            """
            insert into member_cohort_activities (member_id, cohort_id, status, created_at, updated_at)
            select distinct m.member_id, mc.cohort_id, if(m.status = 'ACTIVE' and c.is_active, 'ACTIVE', 'ENDED'), now(6), now(6)
            from members m join member_cohorts mc on mc.member_id = m.member_id join cohorts c on c.cohort_id = mc.cohort_id
            where m.status in ('ACTIVE', 'INACTIVE') and m.deleted_at is null
              and not exists (select 1 from member_cohort_activities a where a.member_id = m.member_id and a.cohort_id = mc.cohort_id)
            """.trimIndent(),
        )
    }
}
