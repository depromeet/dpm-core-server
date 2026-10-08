# DB 변경 관리 (`db/pending`)

Flyway/Liquibase 같은 자동 마이그레이션은 쓰지 않는다.  
**JPA 엔티티 + 수동 SQL**로 DB를 맞춘다.

`db/pending`에는 **DDL뿐 아니라 DML도** 들어갈 수 있다.

| 종류 | 예시 |
|------|------|
| DDL | 테이블/컬럼/인덱스 생성·변경 (`CREATE`, `ALTER`, …) |
| DML | 시드·데이터 보정 (`INSERT`, `UPDATE`, …) — 예: 권한 시드 |

한 파일에 DDL과 DML을 같이 넣어도 되고, 목적별로 파일을 나눠도 된다.

## 폴더

| 경로 | 역할 |
|------|------|
| `db/pending/` | 아직 적용 안 했거나, 적용 대기 중인 SQL |
| `db/release/{YYMMDDHHMM}/` | 적용이 끝난 SQL을 옮기는 곳 |

파일명: `YYMMDDHHMM_짧은설명.sql`  
예: `2610062200_session_feedback.sql`, `2609142320_role_system_seed.sql`

## ddl-auto (dev / prod)

dev·prod는 `ddl-auto: validate`다. 엔티티와 DB가 다르면 **서버 기동 실패**하므로, **앱 배포 전에 SQL을 먼저 실행**한다.

## 작업 흐름

1. **SQL 작성** — `db/pending/YYMMDDHHMM_설명.sql` (DDL·DML 모두 가능)
2. PR 머지 전 **dev DB에 SQL 적용**해 이상 없는지 확인 후 PR 머지
3. 운영 배포 시 pending 파일을 **prod DB에 적용**한 뒤 `db/release/{YYMMDDHHMM}/`로 이동


## SQL 작성 규칙

- 상단 헤더: 날짜·제목 / 목적 / 선행 / 후행 / 검증 / 주의
- `[1]`, `[2]` 구역 + `START TRANSACTION` … `COMMIT`
- **재실행 안전하게**: DDL은 `CREATE TABLE IF NOT EXISTS` / `information_schema` 확인 후 `PREPARE`/`EXECUTE`, DML은 이미 있는지 확인 후 insert·update
- 하단 **VERIFY**(읽기 전용) 쿼리
- DDL 시 컬럼 타입·이름은 엔티티와 **정확히** 일치 (`validate` 통과 조건)
- 가능하면 기존 테이블 변경보다 **신규 테이블**로 범위 한정

## 배포 시 체크

```
[ ] pending SQL을 파일명 순서로 적용했는가?
[ ] VERIFY 결과가 기대와 맞는가?
[ ] 그 다음 앱을 배포하는가? (순서 반대면 validate로 기동 실패)
```

적용 순서는 파일명의 `YYMMDDHHMM` 타임스탬프 순이다.  
선행/후행이 헤더에 있으면 그 순서도 따른다.
