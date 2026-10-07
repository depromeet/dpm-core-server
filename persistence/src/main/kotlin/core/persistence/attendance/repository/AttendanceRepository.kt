package core.persistence.attendance.repository

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import core.domain.attendance.port.outbound.query.MemberAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MyDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionRosterQueryModel
import core.domain.team.vo.TeamNumber
import core.entity.attendance.AttendanceEntity
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SelectJoinStep
import org.jooq.Table
import org.jooq.dsl.tables.references.ABSENCE_REASONS
import org.jooq.dsl.tables.references.ABSENCE_REASON_IMAGES
import org.jooq.dsl.tables.references.ATTENDANCES
import org.jooq.dsl.tables.references.COHORTS
import org.jooq.dsl.tables.references.IMAGES
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_COHORTS
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.MEMBER_TEAMS
import org.jooq.dsl.tables.references.ROLES
import org.jooq.dsl.tables.references.SESSIONS
import org.jooq.dsl.tables.references.TEAMS
import org.jooq.impl.DSL.exists
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.notExists
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.selectCount
import org.jooq.impl.DSL.selectOne
import org.jooq.impl.DSL.sum
import org.jooq.impl.DSL.`when`
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class AttendanceRepository(
    private val attendanceJpaRepository: AttendanceJpaRepository,
    private val dsl: DSLContext,
) : AttendancePersistencePort {
    override fun save(attendance: Attendance) {
        attendanceJpaRepository.save(AttendanceEntity.from(attendance))
    }

    override fun findAttendanceBy(
        sessionId: Long,
        memberId: Long,
    ): Attendance? =
        attendanceJpaRepository
            .findBySessionIdAndMemberIdAndDeletedAtIsNull(
                sessionId,
                memberId,
            )?.toDomain()

    override fun findAllBySessionId(sessionId: Long): List<Attendance> =
        attendanceJpaRepository.findAllBySessionIdAndDeletedAtIsNull(sessionId).map { it.toDomain() }

    /** 대상은 [sessionRosterConditions] 로 고르고 팀과 결석 사유서는 스칼라 서브쿼리로 붙여 행이 늘지 않는다. */
    override fun findSessionRoster(
        sessionId: Long,
        cohortId: Long,
    ): List<SessionRosterQueryModel> {
        val isAdminField = isAdminField()
        val teamNumber = teamNumberInCohort(MEMBERS.MEMBER_ID, SESSIONS.COHORT_ID)
        val teamNumberField = teamNumber.`as`(TEAM_NUMBER)
        val absenceReasonField =
            field(
                select(ABSENCE_REASONS.CONTENTS)
                    .from(ABSENCE_REASONS)
                    .where(
                        ABSENCE_REASONS.SESSION_ID.eq(ATTENDANCES.SESSION_ID),
                        ABSENCE_REASONS.MEMBER_ID.eq(ATTENDANCES.MEMBER_ID),
                    ).orderBy(ABSENCE_REASONS.ABSENCE_REASON_ID.desc())
                    .limit(1),
            ).`as`(ABSENCE_REASON)
        val updatedAtField = updatedAtInstant()

        return dsl
            .select(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                teamNumberField,
                isAdminField,
                MEMBERS.PART,
                ATTENDANCES.STATUS,
                ATTENDANCES.ATTENDED_AT,
                updatedAtField,
                absenceReasonField,
            ).from(ATTENDANCES)
            .joinSessionAndMember()
            .join(COHORTS)
            .on(SESSIONS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .where(sessionRosterConditions(sessionId, cohortId))
            .orderBy(teamNumber.asc().nullsLast(), MEMBERS.NAME.asc(), MEMBERS.MEMBER_ID.asc())
            .fetch { record ->
                SessionRosterQueryModel(
                    memberId = record[MEMBERS.MEMBER_ID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = record[teamNumberField],
                    isAdmin = record[isAdminField] ?: false,
                    part = record[MEMBERS.PART],
                    attendanceStatus = record[ATTENDANCES.STATUS]!!,
                    attendedAt = record[ATTENDANCES.ATTENDED_AT],
                    updatedAt = record[updatedAtField],
                    absenceReason = record[absenceReasonField],
                )
            }
    }

    private fun <R : Record> SelectJoinStep<R>.joinSessionAndMember(): SelectJoinStep<R> =
        join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))

    /**
     * 운영진 세션 명단의 대상 조건. [joinSessionAndMember] 를 전제로 한다.
     * 현재 기수 소속이고 삭제되지 않은 멤버의 살아 있는 출석 기록만, (세션, 멤버)당 [isLatestLiveAttendance] 하나만 본다.
     * 멤버 삭제는 hard delete 라 MEMBERS 내부 조인으로 고아 기록도 빠진다.
     */
    private fun sessionRosterConditions(
        sessionId: Long,
        cohortId: Long,
    ): List<Condition> =
        listOf(
            ATTENDANCES.SESSION_ID.eq(sessionId),
            isLatestLiveAttendance(),
            SESSIONS.COHORT_ID.eq(cohortId),
            SESSIONS.DELETED_AT.isNull,
            MEMBERS.DELETED_AT.isNull,
            belongsToCohort(cohortId),
        )

    /**
     * 삭제되지 않은 기록이고, 같은 (세션, 멤버)의 살아 있는 기록이 여러 개면 attendance_id 가 가장 큰 것이다.
     * 명단, 수료 판정 집계, 사람별·세션별 상세가 같은 기록을 보도록 모두 이 조건을 쓴다.
     */
    private fun isLatestLiveAttendance(): Condition {
        val newer = ATTENDANCES.`as`(NEWER_ATTENDANCE)
        return ATTENDANCES.DELETED_AT.isNull.and(
            notExists(
                selectOne()
                    .from(newer)
                    .where(
                        newer.SESSION_ID.eq(ATTENDANCES.SESSION_ID),
                        newer.MEMBER_ID.eq(ATTENDANCES.MEMBER_ID),
                        newer.DELETED_AT.isNull,
                        newer.ATTENDANCE_ID.gt(ATTENDANCES.ATTENDANCE_ID),
                    ),
            ),
        )
    }

    /** 기수 소속(MEMBER_COHORTS). EXISTS 라 소속 중복으로 행이 늘지 않는다. */
    private fun belongsToCohort(cohortId: Long): Condition =
        exists(
            selectOne()
                .from(MEMBER_COHORTS)
                .where(MEMBER_COHORTS.MEMBER_ID.eq(MEMBERS.MEMBER_ID), MEMBER_COHORTS.COHORT_ID.eq(cohortId)),
        )

    // updated_at 은 생성 타입이 LocalDateTime 이라 존을 가정하면 어긋난다. JPA 가 쓴 것과 같은 JDBC 경로(attended_at 과 같음)로 읽는다.
    private fun updatedAtInstant(): Field<Instant?> = ATTENDANCES.UPDATED_AT.coerce(SQLDataType.INSTANT)

    /** 운영진이 바꾼 기록(updatedAt 있음)의 인증 시각은 null 로 읽는다. 운영진 변경이 인증 시각을 지우기 전의 기록도 같다. */
    private fun visibleAttendedAt(): Field<Instant?> =
        `when`(ATTENDANCES.UPDATED_AT.isNull, ATTENDANCES.ATTENDED_AT).`as`(ATTENDED_AT)

    override fun findTeamNumberInCohort(
        memberId: Long,
        cohortId: Long,
    ): Int? =
        dsl
            .select(TEAMS.NUMBER)
            .from(MEMBER_TEAMS)
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(MEMBER_TEAMS.MEMBER_ID.eq(memberId), TEAMS.COHORT_ID.eq(cohortId))
            .orderBy(MEMBER_TEAMS.MEMBER_TEAM_ID.desc())
            .limit(1)
            .fetchOne(TEAMS.NUMBER)

    override fun findMemberAttendances(
        cohortId: Long,
        teamNumbers: List<Int>,
    ): List<MemberAttendanceQueryModel> = findCohortMemberAttendances(cohortId, teamNumbers = teamNumbers)

    /** [findMemberAttendances] 의 팀 필터 없는 대상 수. 소속은 EXISTS 라 중복 소속 행이 있어도 한 번만 센다. */
    override fun countCohortMembers(cohortId: Long): Int =
        dsl.fetchCount(MEMBERS, MEMBERS.DELETED_AT.isNull, belongsToCohort(cohortId))

    override fun findDetailMemberAttendance(
        memberId: Long,
        cohortId: Long,
    ): MemberDetailAttendanceQueryModel? =
        findCohortMemberAttendances(cohortId, memberId = memberId)
            .singleOrNull()
            ?.let {
                MemberDetailAttendanceQueryModel(
                    memberId = it.id,
                    memberName = it.name,
                    teamNumber = it.teamNumber,
                    isAdmin = it.isAdmin,
                    part = it.part,
                    summary = it.summary,
                )
            }

    /**
     * 기수에 소속되고 삭제되지 않은 멤버마다 한 행. 집계는 LEFT JOIN 이라 출석 기록이 없으면 0 이고 분모는 기수 전체 세션 수다.
     * 팀 필터와 표시 팀은 그 기수의 최신 배정 팀이다. 팀 번호(팀 없음은 마지막), 이름, ID 순이다.
     */
    private fun findCohortMemberAttendances(
        cohortId: Long,
        teamNumbers: List<Int> = emptyList(),
        memberId: Long? = null,
    ): List<MemberAttendanceQueryModel> {
        val isAdminField = isAdminField()
        val teamNumber = teamNumberInCohort(MEMBERS.MEMBER_ID, COHORTS.COHORT_ID)
        val teamNumberField = teamNumber.`as`(TEAM_NUMBER)
        val totalSessionCountField = totalSessionCountOf(COHORTS.COHORT_ID)

        return dsl
            .select(
                listOf(
                    MEMBERS.MEMBER_ID,
                    MEMBERS.NAME,
                    teamNumberField,
                    isAdminField,
                    MEMBERS.PART,
                    totalSessionCountField,
                ) + summaryCountFields,
            ).from(MEMBERS)
            .join(COHORTS)
            .on(COHORTS.COHORT_ID.eq(cohortId))
            .leftJoin(attendanceSummary)
            .on(summaryMemberId.eq(MEMBERS.MEMBER_ID), summaryCohortId.eq(COHORTS.COHORT_ID))
            .where(
                listOfNotNull(
                    MEMBERS.DELETED_AT.isNull,
                    belongsToCohort(cohortId),
                    teamNumbers.takeIf { it.isNotEmpty() }?.let { teamNumber.`in`(it) },
                    memberId?.let { MEMBERS.MEMBER_ID.eq(it) },
                ),
            ).orderBy(teamNumber.asc().nullsLast(), MEMBERS.NAME.asc(), MEMBERS.MEMBER_ID.asc())
            .fetch { record ->
                MemberAttendanceQueryModel(
                    id = record[MEMBERS.MEMBER_ID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = TeamNumber(record[teamNumberField]),
                    isAdmin = record[isAdminField] ?: false,
                    part = record[MEMBERS.PART],
                    summary = record.toAttendanceSummary(totalSessionCountField),
                )
            }
    }

    /** 수료 판정은 사람별 조회와 같은 (멤버, 세션 기수) 집계를 쓴다. 기록은 [isLatestLiveAttendance] 하나다. */
    override fun findDetailAttendanceBySession(
        query: GetDetailAttendanceBySessionQuery,
    ): SessionDetailAttendanceQueryModel? {
        val isAdminField = isAdminField()
        val teamNumberField = teamNumberInCohort(MEMBERS.MEMBER_ID, SESSIONS.COHORT_ID).`as`(TEAM_NUMBER)
        val totalSessionCountField = totalSessionCountOf(SESSIONS.COHORT_ID)
        val attendedAtField = visibleAttendedAt()
        val updatedAtField = updatedAtInstant()

        return dsl
            .select(
                listOf(
                    MEMBERS.MEMBER_ID,
                    MEMBERS.NAME,
                    teamNumberField,
                    isAdminField,
                    MEMBERS.PART,
                    totalSessionCountField,
                ) + summaryCountFields +
                    listOf(
                        SESSIONS.SESSION_ID,
                        SESSIONS.WEEK,
                        SESSIONS.EVENT_NAME,
                        SESSIONS.DATE,
                        ATTENDANCES.STATUS,
                        attendedAtField,
                        updatedAtField,
                    ),
            ).from(ATTENDANCES)
            .joinSessionAndMember()
            .join(COHORTS)
            .on(SESSIONS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .leftJoin(attendanceSummary)
            .on(summaryMemberId.eq(ATTENDANCES.MEMBER_ID), summaryCohortId.eq(SESSIONS.COHORT_ID))
            .where(detailAttendanceConditions(query))
            .fetchOne {
                SessionDetailAttendanceQueryModel(
                    memberId = it[MEMBERS.MEMBER_ID]!!,
                    memberName = it[MEMBERS.NAME]!!,
                    teamNumber = TeamNumber(it[teamNumberField]),
                    isAdmin = it[isAdminField] ?: false,
                    part = it[MEMBERS.PART],
                    summary = it.toAttendanceSummary(totalSessionCountField),
                    sessionId = it[SESSIONS.SESSION_ID]!!,
                    sessionWeek = it[SESSIONS.WEEK]!!,
                    sessionEventName = it[SESSIONS.EVENT_NAME]!!,
                    sessionDate = it[SESSIONS.DATE]!!,
                    attendanceStatus = it[ATTENDANCES.STATUS]!!,
                    attendedAt = it[attendedAtField],
                    updatedAt = it[updatedAtField],
                )
            }
    }

    override fun findMemberSessionAttendances(
        memberId: Long,
        cohortId: Long,
    ): List<MemberSessionAttendanceQueryModel> {
        val attendedAtField = visibleAttendedAt()
        val records =
            dsl
                .select(
                    SESSIONS.SESSION_ID,
                    SESSIONS.WEEK,
                    SESSIONS.EVENT_NAME,
                    SESSIONS.DATE,
                    SESSIONS.IS_ONLINE,
                    SESSIONS.PLACE,
                    ATTENDANCES.STATUS,
                    attendedAtField,
                ).from(ATTENDANCES)
                .joinSessionAndMember()
                .where(
                    ATTENDANCES.MEMBER_ID.eq(memberId),
                    isLatestLiveAttendance(),
                    SESSIONS.COHORT_ID.eq(cohortId),
                    SESSIONS.DELETED_AT.isNull,
                ).orderBy(SESSIONS.WEEK.asc(), SESSIONS.DATE.asc())
                .fetch()

        val absenceReasons =
            findAbsenceReasonsBySession(memberId, records.map { it[SESSIONS.SESSION_ID]!! })

        return records.map { record ->
            MemberSessionAttendanceQueryModel(
                sessionId = record[SESSIONS.SESSION_ID]!!,
                sessionWeek = record[SESSIONS.WEEK]!!,
                sessionEventName = record[SESSIONS.EVENT_NAME]!!,
                sessionDate = record[SESSIONS.DATE]!!,
                sessionIsOnline = record[SESSIONS.IS_ONLINE]!!,
                sessionPlace = record[SESSIONS.PLACE].orEmpty(),
                sessionAttendanceStatus = record[ATTENDANCES.STATUS]!!,
                attendedAt = record[attendedAtField],
                absenceReason = absenceReasons[record[SESSIONS.SESSION_ID]!!],
            )
        }
    }

    /** 결석 사유서를 한 번에 조회한다. 같은 세션에 여러 건이면 가장 최근 것을 쓴다. 첨부 이미지는 사유서 id 로 묶어 한 번 더 읽는다. */
    private fun findAbsenceReasonsBySession(
        memberId: Long,
        sessionIds: List<Long>,
    ): Map<Long, MemberSessionAttendanceQueryModel.AbsenceReason> {
        if (sessionIds.isEmpty()) return emptyMap()

        val reasons =
            dsl
                .select(
                    ABSENCE_REASONS.ABSENCE_REASON_ID,
                    ABSENCE_REASONS.SESSION_ID,
                    ABSENCE_REASONS.CONTENTS,
                    ABSENCE_REASONS.STATUS,
                ).from(ABSENCE_REASONS)
                .where(
                    ABSENCE_REASONS.MEMBER_ID.eq(memberId),
                    ABSENCE_REASONS.SESSION_ID.`in`(sessionIds),
                ).orderBy(ABSENCE_REASONS.ABSENCE_REASON_ID.asc())
                .fetch()
                .associate { record ->
                    record[ABSENCE_REASONS.SESSION_ID]!! to
                        MemberSessionAttendanceQueryModel.AbsenceReason(
                            id = record[ABSENCE_REASONS.ABSENCE_REASON_ID]!!,
                            contents = record[ABSENCE_REASONS.CONTENTS]!!,
                            status = record[ABSENCE_REASONS.STATUS]!!,
                        )
                }
        if (reasons.isEmpty()) return reasons

        val images = findImagesByAbsenceReason(reasons.values.map { it.id })
        return reasons.mapValues { (_, reason) -> reason.copy(images = images[reason.id].orEmpty()) }
    }

    private fun findImagesByAbsenceReason(
        absenceReasonIds: List<Long>,
    ): Map<Long, List<MemberSessionAttendanceQueryModel.Image>> =
        dsl
            .select(ABSENCE_REASON_IMAGES.ABSENCE_REASON_ID, ABSENCE_REASON_IMAGES.IMAGE_ID, IMAGES.ORIGINAL_FILE_NAME)
            .from(ABSENCE_REASON_IMAGES)
            .leftJoin(IMAGES)
            .on(ABSENCE_REASON_IMAGES.IMAGE_ID.eq(IMAGES.IMAGE_ID))
            .where(ABSENCE_REASON_IMAGES.ABSENCE_REASON_ID.`in`(absenceReasonIds))
            .orderBy(ABSENCE_REASON_IMAGES.ABSENCE_REASON_ID.asc(), ABSENCE_REASON_IMAGES.DISPLAY_ORDER.asc())
            .fetch()
            .groupBy({ it[ABSENCE_REASON_IMAGES.ABSENCE_REASON_ID]!! }) {
                MemberSessionAttendanceQueryModel.Image(
                    imageId = it[ABSENCE_REASON_IMAGES.IMAGE_ID]!!,
                    fileName = it[IMAGES.ORIGINAL_FILE_NAME],
                )
            }

    /** 다른 상세 조회처럼 [isLatestLiveAttendance] 하나를 보고 운영진이 바꾼 기록의 인증 시각은 null 로 준다. */
    override fun findMyDetailAttendanceBySession(query: GetMyAttendanceBySessionQuery): MyDetailAttendanceQueryModel? {
        val attendedAtField = visibleAttendedAt()

        return dsl
            .select(
                ATTENDANCES.STATUS,
                attendedAtField,
                SESSIONS.WEEK,
                SESSIONS.EVENT_NAME,
                SESSIONS.DATE,
                SESSIONS.PLACE,
            ).from(ATTENDANCES)
            .joinSessionAndMember()
            .where(myAttendanceConditions(query))
            .fetchOne {
                MyDetailAttendanceQueryModel(
                    attendanceStatus = it[ATTENDANCES.STATUS]!!,
                    attendedAt = it[attendedAtField],
                    sessionWeek = it[SESSIONS.WEEK]!!,
                    sessionEventName = it[SESSIONS.EVENT_NAME]!!,
                    sessionDate = it[SESSIONS.DATE]!!,
                    sessionPlace = it[SESSIONS.PLACE]!!,
                )
            }
    }

    // jOOQ DSLContext 는 JPA 트랜잭션 밖 커넥션을 쓰므로, 호출 트랜잭션과 함께 커밋되도록 JPA 로 저장한다.
    override fun saveInBatch(attendances: List<Attendance>) {
        if (attendances.isEmpty()) return
        attendanceJpaRepository.saveAll(attendances.map { AttendanceEntity.from(it) })
    }

    override fun recordAttendanceIfAllowed(
        attendanceId: Long,
        status: AttendanceStatus,
        attendedAt: Instant,
    ): Boolean = attendanceJpaRepository.recordAttendanceIfAllowed(attendanceId, status.name, attendedAt) == 1

    override fun updateStatusByAdmin(
        sessionId: Long,
        memberIds: List<Long>,
        status: AttendanceStatus,
        updatedAt: Instant,
    ): Int {
        if (memberIds.isEmpty()) return 0
        // 단일 UPDATE 라 행 잠금은 (session_id, member_id) 인덱스 순서로 잡힌다. 자동 결석 UPDATE 와 같은 순서다.
        val sortedMemberIds = memberIds.distinct().sorted()
        return attendanceJpaRepository.updateStatusByAdmin(sessionId, sortedMemberIds, status.name, updatedAt)
    }

    override fun countActiveAttendances(
        sessionId: Long,
        memberIds: List<Long>,
    ): Int {
        if (memberIds.isEmpty()) return 0
        return attendanceJpaRepository.countActiveMembers(sessionId, memberIds.distinct()).toInt()
    }

    override fun updateStatusByPolicy(
        attendanceId: Long,
        expectedStatus: AttendanceStatus,
        newStatus: AttendanceStatus,
    ): Boolean = attendanceJpaRepository.updateStatusByPolicy(attendanceId, expectedStatus.name, newStatus.name) == 1

    override fun reopenAutoAbsence(attendanceId: Long): Boolean =
        attendanceJpaRepository.reopenAutoAbsence(attendanceId) == 1

    override fun markAutoAbsence(
        sessionId: Long,
        autoAbsentAt: Instant,
    ): Int = attendanceJpaRepository.markAutoAbsence(sessionId, autoAbsentAt)

    override fun softDeleteAllBySessionId(
        sessionId: Long,
        deletedAt: Instant,
    ): Int = attendanceJpaRepository.softDeleteAllBySessionId(sessionId, deletedAt)

    /**
     * 수료 판정 조회들이 공유하는 (멤버, 기수) 단위 출석 집계. 팀과 조인하지 않아 카운트가 곱해지지 않는다.
     * 삭제된 세션/기록은 빼고 (세션, 멤버)당 [isLatestLiveAttendance] 하나만 센다. 분모는 [totalSessionCountOf] 로 따로 구한다.
     */
    private val attendanceSummary: Table<*> =
        select(
            ATTENDANCES.MEMBER_ID.`as`(SUMMARY_MEMBER_ID),
            SESSIONS.COHORT_ID.`as`(SUMMARY_COHORT_ID),
            countWhen(ATTENDANCES.STATUS.eq(AttendanceStatus.PRESENT.name)).`as`(PRESENT_COUNT),
            countWhen(ATTENDANCES.STATUS.eq(AttendanceStatus.LATE.name)).`as`(LATE_COUNT),
            countWhen(ATTENDANCES.STATUS.eq(AttendanceStatus.EXCUSED_ABSENT.name)).`as`(EXCUSED_ABSENT_COUNT),
            countWhen(
                ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(true)),
            ).`as`(ONLINE_ABSENT_COUNT),
            countWhen(
                ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(false)),
            ).`as`(OFFLINE_ABSENT_COUNT),
        ).from(ATTENDANCES)
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .where(isLatestLiveAttendance(), SESSIONS.DELETED_AT.isNull)
            .groupBy(ATTENDANCES.MEMBER_ID, SESSIONS.COHORT_ID)
            .asTable(SUMMARY)

    private val summaryMemberId = attendanceSummary.field(SUMMARY_MEMBER_ID, Long::class.javaObjectType)!!
    private val summaryCohortId = attendanceSummary.field(SUMMARY_COHORT_ID, Long::class.javaObjectType)!!
    private val summaryPresentCount = attendanceSummary.field(PRESENT_COUNT, Int::class.javaObjectType)!!
    private val summaryLateCount = attendanceSummary.field(LATE_COUNT, Int::class.javaObjectType)!!
    private val summaryExcusedAbsentCount = attendanceSummary.field(EXCUSED_ABSENT_COUNT, Int::class.javaObjectType)!!
    private val summaryOnlineAbsentCount = attendanceSummary.field(ONLINE_ABSENT_COUNT, Int::class.javaObjectType)!!
    private val summaryOfflineAbsentCount = attendanceSummary.field(OFFLINE_ABSENT_COUNT, Int::class.javaObjectType)!!
    private val summaryCountFields: List<Field<*>> =
        listOf(
            summaryPresentCount,
            summaryLateCount,
            summaryExcusedAbsentCount,
            summaryOnlineAbsentCount,
            summaryOfflineAbsentCount,
        )

    /** 수료 판정 분모: 기수의 삭제되지 않은 전체 세션 수(아직 열리지 않은 세션 포함). 출석 기록이 없는 멤버도 같은 값이다. */
    private fun totalSessionCountOf(cohortId: Field<Long?>): Field<Int> {
        val cohortSessions = SESSIONS.`as`(COHORT_SESSIONS)
        return field(
            selectCount()
                .from(cohortSessions)
                .where(cohortSessions.COHORT_ID.eq(cohortId), cohortSessions.DELETED_AT.isNull),
        ).`as`(TOTAL_SESSION_COUNT)
    }

    private fun countWhen(condition: Condition) = sum(`when`(condition, inline(1)).otherwise(inline(0)))

    /** 집계는 LEFT JOIN 이라 출석 기록이 없으면 null 이고 0 으로 읽는다. */
    private fun Record.toAttendanceSummary(totalSessionCount: Field<Int>) =
        AttendanceSummaryQueryModel(
            totalSessionCount = this[totalSessionCount] ?: 0,
            presentCount = this[summaryPresentCount] ?: 0,
            lateCount = this[summaryLateCount] ?: 0,
            excusedAbsentCount = this[summaryExcusedAbsentCount] ?: 0,
            onlineAbsentCount = this[summaryOnlineAbsentCount] ?: 0,
            offlineAbsentCount = this[summaryOfflineAbsentCount] ?: 0,
        )

    /** 그 기수에서 멤버의 가장 최근 배정(member_team_id 최대) 팀 번호. 없으면 null. 같은 기수의 이전 팀은 보지 않는다. */
    private fun teamNumberInCohort(
        memberId: Field<Long?>,
        cohortId: Field<Long?>,
    ): Field<Int?> =
        field(
            select(TEAMS.NUMBER)
                .from(MEMBER_TEAMS)
                .join(TEAMS)
                .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
                .where(MEMBER_TEAMS.MEMBER_ID.eq(memberId), TEAMS.COHORT_ID.eq(cohortId))
                .orderBy(MEMBER_TEAMS.MEMBER_TEAM_ID.desc())
                .limit(1),
        )

    private fun isAdminField() =
        exists(
            selectOne()
                .from(MEMBER_ROLES)
                .join(ROLES)
                .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
                .where(MEMBER_ROLES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                .and(MEMBER_ROLES.DELETED_AT.isNull)
                .and(ROLES.NAME.eq(COHORTS.VALUE.concat(inline("기 운영진")))),
        ).`as`("is_admin")

    companion object {
        private const val LATE_COUNT = "late_count"
        private const val ONLINE_ABSENT_COUNT = "online_absent_count"
        private const val OFFLINE_ABSENT_COUNT = "offline_absent_count"
        private const val PRESENT_COUNT = "present_count"
        private const val EXCUSED_ABSENT_COUNT = "excused_absent_count"
        private const val TOTAL_SESSION_COUNT = "total_session_count"
        private const val TEAM_NUMBER = "team_number"
        private const val ATTENDED_AT = "attended_at"
        private const val ABSENCE_REASON = "absence_reason"
        private const val NEWER_ATTENDANCE = "newer_attendance"
        private const val COHORT_SESSIONS = "cohort_sessions"
        private const val SUMMARY = "attendance_summary"
        private const val SUMMARY_MEMBER_ID = "member_id"
        private const val SUMMARY_COHORT_ID = "cohort_id"
    }

    private fun detailAttendanceConditions(query: GetDetailAttendanceBySessionQuery): List<Condition> =
        listOf(
            ATTENDANCES.SESSION_ID.eq(query.sessionId.value),
            ATTENDANCES.MEMBER_ID.eq(query.memberId.value),
            isLatestLiveAttendance(),
            SESSIONS.DELETED_AT.isNull,
        )

    private fun myAttendanceConditions(query: GetMyAttendanceBySessionQuery): List<Condition> =
        listOf(
            ATTENDANCES.SESSION_ID.eq(query.sessionId.value),
            ATTENDANCES.MEMBER_ID.eq(query.memberId.value),
            isLatestLiveAttendance(),
        )
}
