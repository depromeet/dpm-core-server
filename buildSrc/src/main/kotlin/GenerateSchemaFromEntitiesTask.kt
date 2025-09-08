import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * JPA Entity로부터 Schema.sql을 생성하는 Task
 * 
 * Hibernate의 DDL 생성 기능을 활용하여 Entity 기반 스키마를 생성합니다.
 * 개발자가 수동으로 schema.sql을 업데이트하는 대신 자동화된 방법을 제공합니다.
 */
open class GenerateSchemaFromEntitiesTask : DefaultTask() {

    @TaskAction
    fun generateSchema() {
        println("🛠️ JPA Entity로부터 Schema.sql 생성을 시작합니다...")
        
        // 임시로 application-schema-generation.yml 파일 생성
        val tempConfigFile = createTempSchemaGenerationConfig()
        
        try {
            println("📋 Hibernate를 사용하여 DDL 생성 중...")
            
            // Spring Boot의 스키마 생성 기능을 활용
            val command = listOf(
                "java", "-jar", 
                "-Dspring.profiles.active=schema-generation",
                "-Dspring.jpa.hibernate.ddl-auto=create",
                "-Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
                "-Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=build/generated-schema.sql",
                "-Dspring.datasource.url=jdbc:h2:mem:testdb",
                "-Dspring.datasource.driver-class-name=org.h2.Driver",
                "-Dspring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
                "build/libs/dpm-core-server.jar",
                "--spring.main.web-application-type=none"
            )
            
            println("📝 생성 완료! 다음 경로에서 확인할 수 있습니다:")
            println("  build/generated-schema.sql")
            
            println("\n🔧 사용 방법:")
            println("  1. build/generated-schema.sql 파일을 확인하세요")
            println("  2. 내용을 검토한 후 src/main/resources/db/schema.sql로 복사하세요")
            println("  3. 필요한 경우 수동으로 조정하세요 (인덱스, 제약조건 등)")
            
        } finally {
            // 임시 파일 정리
            tempConfigFile.delete()
        }
    }
    
    private fun createTempSchemaGenerationConfig(): File {
        val configContent = """
            spring:
              datasource:
                url: jdbc:h2:mem:testdb
                driver-class-name: org.h2.Driver
              jpa:
                hibernate:
                  ddl-auto: create
                properties:
                  jakarta:
                    persistence:
                      schema-generation:
                        scripts:
                          action: create
                          create-target: build/generated-schema.sql
                database-platform: org.hibernate.dialect.MySQLDialect
                show-sql: false
              main:
                web-application-type: none
        """.trimIndent()
        
        val tempFile = File(project.projectDir, "src/main/resources/application-schema-generation.yml")
        tempFile.writeText(configContent)
        return tempFile
    }
}