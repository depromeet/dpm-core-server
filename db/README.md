# DB 스키마 관리 (`db/pending`)

Flyway/Liquibase 같은 자동 마이그레이션은 쓰지 않는다.  
**JPA 엔티티 + 수동 SQL**로 스키마를 맞춘다.

## 폴더

| 경로 | 역할 |
|------|------|
| `db/pending/` | 아직 적용 안 했거나, 적용 대기 중인 SQL |
| `db/release/{YYMMDDHHMM}/` | 적용이 끝난 SQL을 옮기는 곳 |

파일명: `YYMMDDHHMM_짧은설명.sql`  
예: `2610062200_session_feedback.sql`

## 환경별 ddl-auto

| 환경 | 설정 | 의미 |
|------|------|------|
| local | `update` | 엔티티 기준으로 테이블/컬럼 자동 생성 |
| dev / prod | `validate` | 엔티티 ≠ DB면 **서버 기동 실패** |

→ **dev/prod는 앱 배포 전에 SQL을 먼저 실행**해야 한다.

## 작업 흐름

1. **엔티티 추가/변경** (`entity` 모듈)
2. **로컬 확인** — `local` 프로필로 기동 (`ddl-auto: update`)
3. **SQL 작성** — `db/pending/YYMMDDHHMM_설명.sql`
4. PR 머지 전 dev db에 SQL 적용해 sql 이상 없는지 확인 후 PR 머지
5. 이후 운영 배포 시 pending에 있는 파일들을 prod DB에 적용 후 `db/release/{YYMMDDHHMM}/`로 이동 


## SQL 작성 규칙

- 상단 헤더: 날짜·제목 / 목적 / 선행 / 후행 / 검증 / 주의
- `[1]`, `[2]` 구역 + `START TRANSACTION` … `COMMIT`
- **재실행 안전하게**: `CREATE TABLE IF NOT EXISTS`, 또는 `information_schema` 확인 후 `PREPARE`/`EXECUTE`
- 하단 **VERIFY**(읽기 전용) 쿼리
- 컬럼 타입·이름은 엔티티와 **정확히** 일치 (`validate` 통과 조건)
- 가능하면 기존 테이블 변경보다 **신규 테이블**로 범위 한정

## 배포 시 체크

```
[ ] pending SQL을 파일명 순서로 적용했는가?
[ ] VERIFY 결과가 기대와 맞는가?
[ ] 그 다음 앱을 배포하는가? (순서 반대면 validate로 기동 실패)
```

적용 순서는 파일명의 `YYMMDDHHMM` 타임스탬프 순이다.  
선행/후행이 헤더에 있으면 그 순서도 따른다.
