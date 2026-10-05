package core.application.attendance.presentation.controller

import core.application.attendance.presentation.response.DetailAttendancesBySessionResponse
import core.application.attendance.presentation.response.DetailMemberAttendancesResponse
import core.application.attendance.presentation.response.MemberAttendancesResponse
import core.application.attendance.presentation.response.MyAbsenceReasonResponse
import core.application.attendance.presentation.response.MyDetailAttendanceBySessionResponse
import core.application.attendance.presentation.response.SessionAbsenceReasonsResponse
import core.application.attendance.presentation.response.SessionAttendancesResponse
import core.application.common.exception.CustomResponse
import core.application.image.presentation.response.ImageUrlResponse
import core.domain.attendance.enums.AttendanceStatus
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "Attendance Query", description = "출석 조회 API")
interface AttendanceQueryApi {
    @Operation(
        summary = "세션별 출석 조회",
        description = "세션에 대한 출석을 조회합니다. 요청 시 출석상태, 팀, 이름, 커서 ID를 기준으로 필터링할 수 있습니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션별 출석 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "출석 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "members": [
                                                {
                                                    "id": 1,
                                                    "name": "신민철",
                                                    "teamNumber": 1,
                                                    "isAdmin": false,
                                                    "part": "SERVER",
                                                    "attendanceStatus": "PRESENT"
                                                },
                                                {
                                                    "id": 2,
                                                    "name": "이정호",
                                                    "teamNumber": 2,
                                                    "isAdmin": false,
                                                    "part": "WEB",
                                                    "attendanceStatus": "LATE"
                                                }
                                            ],
                                            "filter": {
                                              "teamNumber": 7,
                                              "isMyTeam": false
                                            },
                                            "hasNextPage": false,
                                            "nextCursorId": null,
                                            "totalElements": 26
                                        }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getAttendancesBySessionId(
        sessionId: SessionId,
        memberId: MemberId,
        statuses: List<AttendanceStatus>?,
        teams: List<Int>?,
        name: String?,
        onlyMyTeam: Boolean?,
        page: Int,
        size: Int,
    ): CustomResponse<SessionAttendancesResponse>

    @Operation(
        summary = "사람별 출석 조회",
        description = "사람별 출석을 조회합니다. 요청 시 출석상태, 팀, 이름, 커서 ID를 기준으로 필터링할 수 있습니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사람별 출석 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "출석 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "members": [
                                                {
                                                    "id": 1,
                                                    "name": "신민철",
                                                    "teamNumber": 1,
                                                    "isAdmin": false,
                                                    "part": "SERVER",
                                                    "attendanceStatus": "AT_RISK"
                                                },
                                                {
                                                    "id": 1,
                                                    "name": "이정호",
                                                    "teamNumber": 2,
                                                    "isAdmin": false,
                                                    "part": "WEB",
                                                    "attendanceStatus": "NORMAL"
                                                }
                                            ],
                                            "filter": {
                                              "teamNumber": 7,
                                              "isMyTeam": false
                                            },
                                            "hasNextPage": false,
                                            "nextCursorId": null,
                                            "totalElements": 26
                                        }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getMemberAttendances(
        memberId: MemberId,
        statuses: List<AttendanceStatus>?,
        teams: List<Int>?,
        name: String?,
        onlyMyTeam: Boolean?,
        page: Int,
        size: Int,
    ): CustomResponse<MemberAttendancesResponse>

    @Operation(
        summary = "세션별 개인 출석 상세 조회",
        description = "세션별 개인 출석을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션별 개인 출석 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션별 개인 출석 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "member": {
                                                "id": 1,
                                                "name": "신민철",
                                                "teamNumber": 2,
                                                "isAdmin": false,
                                                "part": "SERVER",
                                                "attendanceStatus": "NORMAL"
                                            },
                                            "session": {
                                                "id": 1,
                                                "week": 2,
                                                "eventName": "2주차 세션",
                                                "date": "2025-08-09T14:00:00.000000"
                                            },
                                            "attendance": {
                                                "status": "LATE",
                                                "attendedAt": "2025-08-09T14:05:12.000000",
                                                "updatedAt": "2025-08-09T14:05:12.000000"
                                            }
                                        }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getAttendanceBySessionIdAndMemberId(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<DetailAttendancesBySessionResponse>

    @Operation(
        summary = "세션별 나의 출석 상세 조회",
        description = "세션별 나의 출석을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션별 나의 출석 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션별 나의 출석 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "attendance": {
                                                "status": "PRESENT",
                                                "attendedAt": "2025-08-09T14:05:12.000000"
                                            },
                                            "session": {
                                                "week": 1,
                                                "eventName": "디프만 17기 OT",
                                                "date": "2025-08-09T14:00:00.000000",
                                                "place": "공덕"
                                            }
                                        }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getMyAttendanceBySessionId(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<MyDetailAttendanceBySessionResponse>

    @Operation(
        summary = "사람별 출석 상세 조회",
        description = "사람별 출석을 상세하게 조회합니다. " + MEMBER_ATTENDANCE_OVERVIEW_DESCRIPTION,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사람별 출석 상세 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "사람별 출석 상세 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "member": {
                                                "id": 1,
                                                "name": "신민철",
                                                "teamNumber": 2,
                                                "isAdmin": false,
                                                "part": "SERVER",
                                                "attendanceStatus": "NORMAL"
                                            },
                                            "attendance": {
                                                "presentCount": 1,
                                                "lateCount": 1,
                                                "excusedAbsentCount": 0,
                                                "absentCount": 1
                                            },
                                            "sessions": [
                                                {
                                                    "id": 1,
                                                    "week": 1,
                                                    "eventName": "디프만 17기 OT",
                                                    "date": "2025-08-02T14:00:00.000000",
                                                    "attendanceStatus": "PRESENT",
                                                    "attendedAt": "2025-08-02T13:58:21.000000",
                                                    "isOnline": false,
                                                    "place": "공덕 창업허브",
                                                    "absenceReason": null
                                                },
                                                {
                                                    "id": 6,
                                                    "week": 2,
                                                    "eventName": "2주차 세션",
                                                    "date": "2025-08-09T14:00:00.000000",
                                                    "attendanceStatus": "LATE",
                                                    "attendedAt": "2025-08-09T14:09:12.000000",
                                                    "isOnline": true,
                                                    "place": "온라인",
                                                    "absenceReason": null
                                                },
                                                {
                                                    "id": 9,
                                                    "week": 3,
                                                    "eventName": "3주차 세션",
                                                    "date": "2025-08-16T14:00:00.000000",
                                                    "attendanceStatus": "ABSENT",
                                                    "attendedAt": null,
                                                    "isOnline": false,
                                                    "place": "공덕 창업허브",
                                                    "absenceReason": {
                                                        "id": 3,
                                                        "contents": "병원 진료",
                                                        "status": "PENDING",
                                                        "imageIds": [12, 15],
                                                        "images": [
                                                            { "imageId": 12, "fileName": "진단서.jpg" },
                                                            { "imageId": 15, "fileName": null }
                                                        ]
                                                    }
                                                }
                                            ]
                                        }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getDetailMemberAttendances(memberId: MemberId): CustomResponse<DetailMemberAttendancesResponse>

    @Operation(
        summary = "나의 출석 리스트 상세 조회",
        description = "나의 출석 리스트를 상세하게 조회합니다. " + MEMBER_ATTENDANCE_OVERVIEW_DESCRIPTION,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "나의 출석 리스트 상세 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "나의 출석 리스트 상세 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "member": {
                                                "id": 1,
                                                "name": "신민철",
                                                "teamNumber": 2,
                                                "isAdmin": false,
                                                "part": "SERVER",
                                                "attendanceStatus": "NORMAL"
                                            },
                                            "attendance": {
                                                "presentCount": 1,
                                                "lateCount": 1,
                                                "excusedAbsentCount": 0,
                                                "absentCount": 1
                                            },
                                            "sessions": [
                                                {
                                                    "id": 1,
                                                    "week": 1,
                                                    "eventName": "디프만 17기 OT",
                                                    "date": "2025-08-02T14:00:00.000000",
                                                    "attendanceStatus": "PRESENT",
                                                    "attendedAt": "2025-08-02T13:58:21.000000",
                                                    "isOnline": false,
                                                    "place": "공덕 창업허브",
                                                    "absenceReason": null
                                                },
                                                {
                                                    "id": 6,
                                                    "week": 2,
                                                    "eventName": "2주차 세션",
                                                    "date": "2025-08-09T14:00:00.000000",
                                                    "attendanceStatus": "LATE",
                                                    "attendedAt": "2025-08-09T14:09:12.000000",
                                                    "isOnline": true,
                                                    "place": "온라인",
                                                    "absenceReason": null
                                                },
                                                {
                                                    "id": 9,
                                                    "week": 3,
                                                    "eventName": "3주차 세션",
                                                    "date": "2025-08-16T14:00:00.000000",
                                                    "attendanceStatus": "ABSENT",
                                                    "attendedAt": null,
                                                    "isOnline": false,
                                                    "place": "공덕 창업허브",
                                                    "absenceReason": {
                                                        "id": 3,
                                                        "contents": "병원 진료",
                                                        "status": "PENDING",
                                                        "imageIds": [12, 15],
                                                        "images": [
                                                            { "imageId": 12, "fileName": "진단서.jpg" },
                                                            { "imageId": 15, "fileName": null }
                                                        ]
                                                    }
                                                }
                                            ]
                                        }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getMyDetailAttendances(memberId: MemberId): CustomResponse<DetailMemberAttendancesResponse>

    @Operation(
        summary = "내 결석 사유서 조회",
        description =
            "로그인한 디퍼가 해당 세션에 제출한 결석 사유서를 조회합니다. 제출 이력이 없으면 data 가 비어있습니다. " +
                "imageIds 는 첨부 이미지 id(표시 순서, 없으면 [])이며 조회 URL 은 GET /v3/images/{imageId} 로 받습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 결석 사유서 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "내 결석 사유서",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "contents": "병원 진료",
                                            "status": "PENDING",
                                            "imageIds": [12, 15],
                                            "createdAt": "2025-08-16T15:00:00",
                                            "updatedAt": null
                                        }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getMyAbsenceReason(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<MyAbsenceReasonResponse>

    @Operation(
        summary = "세션 결석 사유서 목록 조회 (운영진)",
        description =
            "운영진이 해당 세션에 제출된 모든 결석 사유서를 제출자 이름과 함께 조회합니다. " +
                "reasons[].imageIds 는 첨부 이미지 id(표시 순서, 없으면 [])이며 " +
                "조회 URL 은 GET /v3/sessions/{sessionId}/absence-reasons/{memberId}/images/{imageId} 로 받습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션 결석 사유서 목록 조회 성공",
            ),
        ],
    )
    fun getSessionAbsenceReasons(sessionId: SessionId): CustomResponse<SessionAbsenceReasonsResponse>

    @Operation(
        summary = "결석 사유서 첨부 이미지 조회 URL 발급 (운영진)",
        description =
            "해당 세션·멤버의 결석 사유서에 지금 첨부된 이미지의 만료 시간이 있는 조회 URL 을 받습니다. " +
                "첨부가 해제됐거나 사유서가 삭제됐으면 404 입니다. url 은 인증 헤더 없이 img 태그로 바로 표시할 수 있으며 " +
                "expiresAt 이 지나면 다시 발급받아야 합니다. 응답은 캐시하지 않습니다(private, no-store).",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "이미지 조회 URL 발급 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                name = "이미지 조회 URL",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "url": "https://objectstorage.ap-chuncheon-1.oraclecloud.com/p/.../o/...",
                                            "expiresAt": "2025-08-16T06:05:00Z"
                                        }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "403", description = "운영진 권한(update:attendance) 없음"),
            ApiResponse(responseCode = "404", description = "IMAGE-404-01 사유서가 없거나 그 사유서에 첨부된 이미지가 아님"),
            ApiResponse(responseCode = "503", description = "이미지 저장소 사용 불가"),
        ],
    )
    fun getAbsenceReasonImage(
        sessionId: SessionId,
        memberId: MemberId,
        imageId: ImageId,
    ): ResponseEntity<CustomResponse<ImageUrlResponse>>

    @Operation(
        summary = "결석 사유서 첨부 이미지 다운로드 (운영진)",
        description =
            "해당 세션·멤버의 결석 사유서에 지금 첨부된 이미지를 원본 파일명으로 내려받도록 1분짜리 다운로드 URL 로 302 리다이렉트합니다. " +
                "<a href> 로 바로 연결할 수 있습니다(로그인 쿠키로 인증). 첨부가 해제됐거나 사유서가 삭제됐으면 404 입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "302",
                description = "Location: 다운로드 URL (Content-Disposition: attachment). Cache-Control: no-store, private",
            ),
            ApiResponse(responseCode = "403", description = "운영진 권한(update:attendance) 없음"),
            ApiResponse(responseCode = "404", description = "IMAGE-404-01 사유서가 없거나 그 사유서에 첨부된 이미지가 아님"),
            ApiResponse(responseCode = "503", description = "이미지 저장소 사용 불가"),
        ],
    )
    fun downloadAbsenceReasonImage(
        sessionId: SessionId,
        memberId: MemberId,
        imageId: ImageId,
    ): ResponseEntity<Void>
}

private const val MEMBER_ATTENDANCE_OVERVIEW_DESCRIPTION =
    "member.attendanceStatus 는 조회 시점에 계산한 수료 판정(NORMAL/AT_RISK/IMPOSSIBLE)이다. " +
        "결석 1회, 지각 0.5회로 환산하고 인정 결석은 출석으로, 미인증(PENDING)과 조퇴(EARLY_LEAVE)는 0으로 본다. " +
        "IMPOSSIBLE: 남은 세션을 모두 출석해도 출석률 80% 미만(분모는 해당 기수의 삭제되지 않은 전체 세션 수), " +
        "환산 결석 4회 초과(4.5회부터), 오프라인 결석 3회 이상 중 하나. " +
        "AT_RISK: 환산 결석 3회 이상 또는 오프라인 결석 2회. " +
        "sessions[].attendedAt 은 실제 출석 인증 시각이며 인증하지 않았으면 null 이다. " +
        "sessions[].place 는 온라인 세션이면 \"온라인\", 오프라인이면 저장된 장소명이다. " +
        "sessions[].absenceReason 은 해당 세션에 제출한 결석 사유서이며 없으면 null 이다. " +
        "absenceReason.images 는 첨부 이미지 id 와 원본 파일명(표시 순서, 없으면 [])이며, " +
        "파일명 없이 올렸거나 파일명 저장 전에 올린 이미지는 fileName 이 null 이다. " +
        "absenceReason.imageIds 는 같은 순서의 id 목록으로 호환을 위해 유지한다."
