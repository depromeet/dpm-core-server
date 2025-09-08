import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * JPA Entity와 Schema.sql 동기화를 위한 간단한 검증 Task
 * 
 * 1. 알려진 불일치 사항들을 체크
 * 2. 개발자에게 수정이 필요한 부분 알림
 * 3. 향후 더 정교한 검증 로직으로 확장 가능
 */
open class ValidateSchemaTask : DefaultTask() {

    @TaskAction
    fun validateSchema() {
        println("🔍 JPA Entity와 Schema.sql 동기화 검증을 시작합니다...")

        val issues = mutableListOf<String>()
        
        // 현재 알려진 불일치 사항들 체크
        issues.addAll(checkGatheringMembersTable())
        issues.addAll(checkBillsTable())
        
        if (issues.isNotEmpty()) {
            println("❌ Schema 불일치가 감지되었습니다:")
            issues.forEach { println("  - $it") }
            
            println("\n🔧 해결 방법:")
            println("  1. 아래 불일치 사항들을 확인하세요")
            println("  2. JPA Entity 또는 schema.sql을 수정하여 일치시키세요")
            println("  3. 수정 후 다시 빌드하여 검증하세요")
            
            if (System.getProperty("schema.validation.fail", "true").toBoolean()) {
                throw GradleException("Schema 동기화 검증 실패. 위의 불일치 사항을 해결해주세요.")
            } else {
                println("\n⚠️  경고: 검증 실패했지만 -Dschema.validation.fail=false 옵션으로 인해 빌드를 계속 진행합니다.")
            }
        } else {
            println("✅ Schema 동기화 검증 성공!")
        }
    }
    
    private fun checkGatheringMembersTable(): List<String> {
        val issues = mutableListOf<String>()
        
        val entityFile = File(project.projectDir, 
            "src/main/kotlin/com/server/dpmcore/gathering/gatheringMember/infrastructure/entity/GatheringMemberEntity.kt")
        val schemaFile = File(project.projectDir, "src/main/resources/db/schema.sql")
        
        if (!entityFile.exists() || !schemaFile.exists()) {
            return issues
        }
        
        val entityContent = entityFile.readText()
        val schemaContent = schemaFile.readText()
        
        // is_joined 컬럼 nullable 체크
        if (entityContent.contains("val isJoined: Boolean? = null") && 
            schemaContent.contains("`is_joined`           bit(1) NOT NULL")) {
            issues.add("gathering_members 테이블의 is_joined 컬럼: Entity에서는 nullable이지만 Schema에서는 NOT NULL입니다.")
        }
        
        return issues
    }
    
    private fun checkBillsTable(): List<String> {
        val issues = mutableListOf<String>()
        
        val entityFile = File(project.projectDir, 
            "src/main/kotlin/com/server/dpmcore/bill/bill/infrastructure/entity/BillEntity.kt")
        val schemaFile = File(project.projectDir, "src/main/resources/db/schema.sql")
        
        if (!entityFile.exists() || !schemaFile.exists()) {
            return issues
        }
        
        val entityContent = entityFile.readText()
        val schemaContent = schemaFile.readText()
        
        // description 컬럼 nullable 체크
        if (entityContent.contains("val description: String,") && 
            schemaContent.contains("`description`     varchar(255) DEFAULT NULL")) {
            issues.add("bills 테이블의 description 컬럼: Entity에서는 non-nullable이지만 Schema에서는 nullable입니다.")
        }
        
        // bill_status 컬럼 존재 체크
        if (entityContent.contains("val billStatus: String") && 
            !schemaContent.contains("`bill_status`")) {
            issues.add("bills 테이블에 bill_status 컬럼이 schema.sql에 누락되었습니다.")
        }
        
        // host_user_id 컬럼 존재 체크
        if (entityContent.contains("val hostUserId: Long") && 
            !schemaContent.contains("`host_user_id`")) {
            issues.add("bills 테이블에 host_user_id 컬럼이 schema.sql에 누락되었습니다.")
        }
        
        return issues
    }
}