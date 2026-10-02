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

`POST /v1/sessions`에서 attendanceStart, lateStart, absentStart를 모두 생략하면 기본값으로 계산한 절대 시각을 저장하고, 모두 보내면 그대로 사용한다. 일부만 보내면 거절한다.

기본값을 바꿔도 이미 만든 세션은 바뀌지 않는다. 기존 세션의 시각은 세션 수정 API로 바꾼다.

## 인증 마감과 자동 결석

마감 후에는 유효한 코드를 입력해도 인증을 저장하지 않는다. 마감 전에 접수된 인증은 자동 결석이 먼저 저장됐더라도 원래 요청 시각대로 반영한다.

자동 결석 스케줄러는 이전 실행 종료 30초 뒤 다시 실행하며, 하한 없이 과거 기수를 포함해 마감이 지난 모든 세션의 미인증 기록을 결석으로 바꾼다. 배포 후 첫 실행에서 과거 세션의 미인증 기록도 결석이 된다. 출석·지각·인정 결석·운영진 변경 기록과 자동 결석 표지가 없는 기존 결석은 바꾸지 않는다.

## 배포와 검증

배포 전에 `db/pending/2610011100_attendance_session_member_index.sql`, `db/pending/2610011200_attendance_auto_absence.sql`을 이 순서로 적용한다.

일반 테스트: `./gradlew :application:test`

MySQL 동시성 테스트(로컬 일회용 `dpm_it*` DB, 스키마를 새로 만든다):

```sh
DPM_IT_MYSQL_URL='jdbc:mysql://127.0.0.1:3307/dpm_it_attendance?serverTimezone=Asia/Seoul' \
DPM_IT_MYSQL_USERNAME=root DPM_IT_MYSQL_PASSWORD='<test password>' \
./gradlew :application:mysqlIntegrationTest
```
