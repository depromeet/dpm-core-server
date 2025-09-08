# JPA Entity와 Schema.sql 동기화 해결책

## 문제 상황

JPA와 JOOQ를 함께 사용하는 환경에서 다음과 같은 문제가 발생할 수 있습니다:

1. **JPA Entity 수정 시 schema.sql 업데이트 누락**: 개발자가 Entity를 수정했지만 schema.sql을 업데이트하지 않아 불일치 발생
2. **JOOQ DSL Class와 실제 Entity 불일치**: schema.sql 기반으로 생성된 JOOQ DSL이 실제 Entity와 달라 런타임 에러 발생
3. **수동 동기화의 한계**: 복잡한 스키마에서 수동으로 일치시키기 어려움

## 해결책

### 1. 자동 스키마 검증 (Gradle Task)

빌드 과정에서 자동으로 JPA Entity와 schema.sql의 일치성을 검증합니다.

```bash
# 스키마 검증 실행
./gradlew validateSchema

# 검증 실패 시에도 빌드 계속 진행 (개발 중에만 사용)
./gradlew build -Dschema.validation.fail=false
```

#### 주요 기능:
- **컴파일 전 자동 검증**: `compileKotlin` task 실행 전 자동으로 스키마 검증
- **상세한 오류 메시지**: 어떤 테이블의 어떤 컬럼이 불일치하는지 명확히 표시
- **개발 모드 지원**: 필요시 검증을 우회할 수 있는 옵션 제공

### 2. Entity 기반 스키마 생성 (예정)

JPA Entity로부터 자동으로 schema.sql을 생성하는 기능:

```bash
# Entity에서 스키마 생성
./gradlew generateSchemaFromEntities
```

### 3. 검증되는 항목들

현재 검증하는 주요 불일치 사항:

#### `gathering_members` 테이블
- `is_joined` 컬럼의 nullable 속성 일치성

#### `bills` 테이블
- `description` 컬럼의 nullable 속성 일치성
- `bill_status` 컬럼 존재 여부
- `host_user_id` 컬럼 존재 여부

## 사용법

### 일반적인 개발 워크플로우

1. **JPA Entity 수정**
   ```kotlin
   @Entity
   class MyEntity(
       // 컬럼 추가/수정
       @Column(name = "new_column", nullable = false)
       val newColumn: String
   )
   ```

2. **빌드 실행**
   ```bash
   ./gradlew build
   ```

3. **검증 실패 시 대응**
   - 오류 메시지 확인
   - schema.sql 수정 또는 Entity 수정
   - 재빌드

### 스키마 불일치 해결 방법

#### Case 1: Entity가 정확한 경우
schema.sql을 Entity에 맞게 수정:

```sql
-- 수정 전
`is_joined` bit(1) NOT NULL

-- 수정 후 (Entity가 nullable인 경우)
`is_joined` bit(1) DEFAULT NULL
```

#### Case 2: schema.sql이 정확한 경우
Entity를 schema.sql에 맞게 수정:

```kotlin
// 수정 전
val isJoined: Boolean? = null

// 수정 후 (schema가 NOT NULL인 경우)
@Column(nullable = false)
val isJoined: Boolean = false
```

## 기술적 세부사항

### 프로젝트 구조
```
buildSrc/
├── build.gradle.kts              # buildSrc 의존성 설정
└── src/main/kotlin/
    ├── ValidateSchemaTask.kt     # 스키마 검증 Task
    └── GenerateSchemaFromEntitiesTask.kt  # 스키마 생성 Task (예정)
```

### 검증 알고리즘
1. **파일 존재성 확인**: Entity 파일과 schema.sql 파일 존재 확인
2. **패턴 매칭**: 정규표현식을 사용하여 특정 패턴 검증
3. **텍스트 분석**: 파일 내용을 분석하여 불일치 사항 탐지

### 확장 가능성
- **더 정교한 DDL 파싱**: SQL Parser를 사용한 정확한 스키마 비교
- **자동 수정 제안**: 발견된 불일치에 대한 수정 제안
- **CI/CD 통합**: GitHub Actions 등에서 자동 검증

## 장점

1. **런타임 오류 방지**: 컴파일 타임에 스키마 불일치 발견
2. **개발 효율성 향상**: 수동 검증 작업 불필요
3. **안정성 증대**: JOOQ와 JPA 간 일관성 보장
4. **팀 협업 개선**: 모든 개발자가 동일한 검증 과정 거침

## 주의사항

- **초기 설정**: 기존 불일치 사항들을 먼저 해결해야 함
- **성능**: 대규모 프로젝트에서는 검증 시간이 다소 소요될 수 있음
- **커스터마이징**: 프로젝트별 특수 요구사항에 맞게 조정 필요

## 향후 개선사항

1. **Hibernate Metadata API 활용**: 더 정확한 스키마 비교
2. **Flyway/Liquibase 통합**: 마이그레이션 도구와의 연동
3. **IDE 플러그인**: IntelliJ IDEA 플러그인 개발
4. **실시간 감지**: 파일 변경 시 실시간 검증