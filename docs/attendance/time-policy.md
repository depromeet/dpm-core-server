# 출석 시간 설정과 자동 결석

관련 이슈: https://github.com/depromeet/dpm-core-server/issues/573

## 환경변수 기본값

| 환경변수 | 기본값 | 의미 |
|---|---|---|
| ATTENDANCE_OPEN_MINUTES_BEFORE_START | 10 | 시작 몇 분 전 인증 시작 |
| ATTENDANCE_LATE_AFTER_START_MINUTES | 15 | 시작 몇 분 후 지각 |
| ATTENDANCE_ABSENT_AFTER_START_MINUTES | 30 | 시작 몇 분 후 마감 |

값은 0~1440분이며 인증 시작 < 지각 시작 < 마감 순서를 기동 시 검증한다. 변경은 재시작 후 적용된다.

세션 시작을 T, 서버가 인증 요청을 받은 시각을 t라고 하면 기본값은 다음과 같다.

- 출석: T-10분 ≤ t < T+15분
- 지각: T+15분 ≤ t < T+30분
- 인증 마감: T+30분 ≤ t

## 세션 생성 및 변경

`POST /v3/sessions`에서 attendanceStart, lateStart, absentStart를 모두 생략하면 기본값으로 계산한 절대 시각을 저장하고, 모두 보내면 그대로 사용한다. 일부만 보내면 거절한다.

기본값을 바꿔도 이미 만든 세션은 바뀌지 않는다. 기존 세션의 시각은 세션 수정 API(`PATCH /v3/sessions`, 출석 시작만은 `PATCH /v3/sessions/{sessionId}/attendance-time`)로 바꾼다.

세션·출석 판정 규칙이 바뀐 API 는 `/v3` 로 옮겼다.

## 인증 마감과 자동 결석

마감 후에는 유효한 코드를 입력해도 인증(`POST /v3/sessions/{sessionId}/attendances`)을 저장하지 않는다. 마감 전에 접수된 인증은 자동 결석이 먼저 저장됐더라도 원래 요청 시각대로 반영한다.

자동 결석 스케줄러는 매일 19시(KST)에 한 번 실행하며(실패한 세션은 짧은 간격으로 최대 3번까지 시도), 19시 이후 마감되는 세션은 다음 날 처리된다. 실행마다 활성 기수(`is_active`)에서 하한 없이 마감이 지난 세션의 미인증(현재 `PENDING`) 기록을 결석으로 바꾼다. 활성 기수가 없으면 실행을 건너뛰고, 지난 기수 세션은 처리하지 않는다. 삭제된 세션·출석 기록은 대상이 아니다.

대상 판단은 현재 상태만 본다. 운영진이 `PENDING`으로 되돌린 기록도 마감이 지났으면 결석이 되며, 이 결석은 운영진 기록으로 보고 마감 연장 시 되돌리지 않는다. 출석·지각·인정 결석 등 `PENDING`이 아닌 기록과 자동 결석 표지가 없는 기존 결석은 바꾸지 않는다.

## 배포와 검증

배포 전에 `db/pending/2610011100_attendance_session_member_index.sql`, `db/pending/2610011200_attendance_auto_absence.sql`을 이 순서로 적용한다.

일반 테스트: `./gradlew :application:test`

MySQL 동시성 테스트(로컬 일회용 `dpm_it*` DB, 스키마를 새로 만든다):

```sh
DPM_IT_MYSQL_URL='jdbc:mysql://127.0.0.1:3307/dpm_it_attendance?serverTimezone=Asia/Seoul' \
DPM_IT_MYSQL_USERNAME=root DPM_IT_MYSQL_PASSWORD='<test password>' \
./gradlew :application:mysqlIntegrationTest
```
