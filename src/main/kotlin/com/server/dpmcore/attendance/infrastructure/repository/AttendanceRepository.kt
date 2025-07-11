package com.server.dpmcore.attendance.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.singleQuery
import com.server.dpmcore.attendance.domain.model.Attendance
import com.server.dpmcore.attendance.domain.port.outbound.AttendancePersistencePort
import com.server.dpmcore.attendance.infrastructure.entity.AttendanceEntity
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId
import org.springframework.stereotype.Repository

@Repository
class AttendanceRepository(
    private val attendanceJpaRepository: AttendanceJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : AttendancePersistencePort {
    override fun findAttendanceBy(
        sessionId: SessionId,
        memberId: MemberId,
    ): Attendance =
        queryFactory
            .singleQuery<AttendanceEntity> {
                select(entity(AttendanceEntity::class))
                from(entity(AttendanceEntity::class))
                whereAnd(
                    col(AttendanceEntity::sessionId).equal(sessionId.value),
                    col(AttendanceEntity::memberId).equal(memberId.value),
                )
            }.toDomain()

    override fun save(attendance: Attendance) {
        attendanceJpaRepository.save(AttendanceEntity.from(attendance))
    }
}
