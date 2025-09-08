# JPA Entity와 Schema.sql 동기화 검증 데모

이 문서는 구현된 스키마 검증 기능이 제대로 작동하는지 확인하는 테스트 시나리오입니다.

## 시나리오 1: 정상적인 경우
현재 상태에서 검증을 실행하면 성공해야 합니다.

```bash
./gradlew validateSchema
```

예상 결과:
```
🔍 JPA Entity와 Schema.sql 동기화 검증을 시작합니다...
✅ Schema 동기화 검증 성공!
```

## 시나리오 2: 불일치 발견 테스트

실제 불일치가 있을 때의 동작을 확인하기 위해 일시적으로 schema.sql을 수정할 수 있습니다.

### 테스트 1: is_joined 컬럼 불일치
`gathering_members` 테이블의 `is_joined`을 다시 NOT NULL로 변경하면:

```sql
`is_joined` bit(1) NOT NULL,  -- Entity에서는 nullable
```

검증 결과:
```
❌ Schema 불일치가 감지되었습니다:
  - gathering_members 테이블의 is_joined 컬럼: Entity에서는 nullable이지만 Schema에서는 NOT NULL입니다.
```

### 테스트 2: 컬럼 누락
`bills` 테이블에서 `bill_status` 컬럼을 제거하면:

검증 결과:
```
❌ Schema 불일치가 감지되었습니다:
  - bills 테이블에 bill_status 컬럼이 schema.sql에 누락되었습니다.
```

## 개발 모드 테스트

검증 실패시에도 빌드를 계속 진행하고 싶은 경우:

```bash
./gradlew build -Dschema.validation.fail=false
```

이 경우 경고 메시지만 출력하고 빌드를 계속 진행합니다.

## 자동 통합 테스트

일반적인 빌드 과정에서 자동으로 검증이 실행됩니다:

```bash
./gradlew build
```

`compileKotlin` 작업 전에 `validateSchema`가 자동으로 실행되어 스키마 불일치를 사전에 감지합니다.