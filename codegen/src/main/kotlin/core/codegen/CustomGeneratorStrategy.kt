package core.codegen

import org.jooq.codegen.DefaultGeneratorStrategy
import org.jooq.meta.ColumnDefinition
import org.jooq.meta.Definition

class CustomGeneratorStrategy : DefaultGeneratorStrategy() {
    override fun getJavaIdentifier(definition: Definition): String {
        if (definition is ColumnDefinition) {
            return definition.name
        }
        return super.getJavaIdentifier(definition)
    }
}

