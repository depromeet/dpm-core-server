package com.server.dpmcore.schema

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File

/**
 * Schema 검증 로직 테스트
 */
class SchemaValidationTest {

    @Test
    fun `schema 파일들이 존재하는지 확인`() {
        assertDoesNotThrow {
            val schemaFile = File("src/main/resources/db/schema.sql")
            assert(schemaFile.exists()) { "schema.sql 파일이 존재하지 않습니다." }
        }
    }

    @Test
    fun `주요 Entity 파일들이 존재하는지 확인`() {
        assertDoesNotThrow {
            val entityFiles = listOf(
                "src/main/kotlin/com/server/dpmcore/gathering/gatheringMember/infrastructure/entity/GatheringMemberEntity.kt",
                "src/main/kotlin/com/server/dpmcore/bill/bill/infrastructure/entity/BillEntity.kt"
            )
            
            entityFiles.forEach { path ->
                val entityFile = File(path)
                assert(entityFile.exists()) { "$path 파일이 존재하지 않습니다." }
            }
        }
    }

    @Test
    fun `스키마 파일에 필수 테이블들이 포함되어 있는지 확인`() {
        assertDoesNotThrow {
            val schemaFile = File("src/main/resources/db/schema.sql")
            if (schemaFile.exists()) {
                val content = schemaFile.readText()
                
                val requiredTables = listOf(
                    "gathering_members",
                    "bills",
                    "members",
                    "teams"
                )
                
                requiredTables.forEach { tableName ->
                    assert(content.contains("CREATE TABLE `$tableName`")) { 
                        "$tableName 테이블이 schema.sql에 정의되지 않았습니다." 
                    }
                }
            }
        }
    }
}