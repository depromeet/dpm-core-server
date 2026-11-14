---
name: db-migration
description: DB 스키마·데이터 변경이 필요할 때. db/pending/ SQL 파일 작성 규칙, 파일명 형식, 헤더 구조, 배포 순서.
---

# DB Migration

> **이 스킬을 쓰는 경우:** 신규 테이블·컬럼·인덱스 추가, 데이터 시드·보정이 필요할 때.

Flyway/Liquibase 없이 **수동 SQL**로 관리한다. `ddl-auto: validate` 환경이므로 **앱 배포 전에 SQL을 먼저 적용**해야 서버가 뜬다.

---

## 폴더 구조

```
db/
├── pending/              ← 작업할 파일 여기에 작성
└── release/{YYMMDDHHMM}/ ← 적용 완료 후 이동
```

---

## 파일명

```
YYMMDDHHMM_짧은설명.sql
```

예: `2611121430_session_feedback_form_disabled.sql`

타임스탬프는 작성 시각 기준. 적용 순서는 파일명의 타임스탬프 오름차순.

---

## 파일 헤더 형식

```sql
-- =============================================================================
-- YYYY-MM-DD · 제목 (#이슈번호)
-- =============================================================================
-- 목적: 한두 줄 설명
-- 선행: 없음 / 혹은 "2611121000_xxx.sql 이후"
-- 후행: 없음 / 혹은 "앱 배포 후 추가 DML 필요"
-- 검증: 파일 하단 VERIFY 섹션
-- 주의: ddl-auto=validate → 이 SQL 적용 후 앱 배포
-- =============================================================================
```

---

## 작성 규칙

### DDL

```sql
-- [1] 테이블 생성
START TRANSACTION;

CREATE TABLE IF NOT EXISTS session_feedback_forms (
    session_feedback_form_id BIGINT      NOT NULL AUTO_INCREMENT,
    session_id               BIGINT      NOT NULL,
    -- ...
    created_at               DATETIME(6) NULL,
    updated_at               DATETIME(6) NULL,
    deleted_at               DATETIME(6) NULL,
    PRIMARY KEY (session_feedback_form_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

COMMIT;
```

컬럼 추가 (재실행 안전하게):
```sql
START TRANSACTION;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'sessions'
      AND COLUMN_NAME  = 'disabled'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sessions ADD COLUMN disabled BIT(1) NOT NULL DEFAULT 0',
    'SELECT ''already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

COMMIT;
```

### DML (시드·보정)

```sql
-- [2] 초기 데이터
START TRANSACTION;

INSERT INTO permissions (name, description)
SELECT 'read:session', '세션 조회'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'read:session');

COMMIT;
```

### VERIFY (파일 하단, 읽기 전용)

```sql
-- =============================================================================
-- VERIFY (읽기 전용 — 적용 결과 확인용)
-- =============================================================================
SELECT COUNT(*) AS table_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'session_feedback_forms';

-- 기대값: 1
```

---

## Entity와 타입 일치 규칙

`ddl-auto: validate`이므로 컬럼 타입·이름이 JPA Entity와 **정확히** 맞아야 한다.

| Kotlin 타입 | MySQL 타입 |
|---|---|
| `String` | `VARCHAR(255)` |
| `Long` | `BIGINT` |
| `Boolean` / `bit` | `BIT(1)` |
| `LocalDateTime` | `DATETIME(6)` |
| `Enum` (string) | `VARCHAR(50)` |

---

## 작업 흐름

1. `db/pending/YYMMDDHHMM_설명.sql` 작성
2. 로컬 DB에 적용해서 서버 기동 확인
3. PR에 SQL 파일 포함
4. PR 머지 전 **dev DB에 먼저 적용**
5. 운영 배포 시 **prod DB 적용 → 앱 배포** 순서
6. 완료 후 `db/release/{YYMMDDHHMM}/` 으로 이동

---

## 체크리스트

```
[ ] 파일명 타임스탬프가 작성 시각 기준인가?
[ ] 헤더에 목적·선행·후행·주의 작성했는가?
[ ] DDL이 IF NOT EXISTS / information_schema 체크로 재실행 안전한가?
[ ] DML이 중복 방지 처리됐는가? (WHERE NOT EXISTS)
[ ] Entity 컬럼 타입과 일치하는가?
[ ] VERIFY 쿼리가 하단에 있는가?
[ ] SQL 적용 → 앱 배포 순서를 지켰는가?
```
