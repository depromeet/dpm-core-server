package com.server.dpmcore.common.converter

import com.server.dpmcore.common.exception.CustomResponse
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class ResponseModelConverter : ModelConverter {
    override fun resolve(
        type: AnnotatedType,
        context: ModelConverterContext,
        chain: MutableIterator<ModelConverter>,
    ): Schema<*>? {
        val root = type.type ?: return next(chain, type, context)

        val unwrapped = unwrapKnownWrappers(root)

        val p = unwrapped as? ParameterizedType ?: return next(chain, type, context)
        val raw = p.rawType as? Class<*> ?: return next(chain, type, context)
        if (raw != CustomResponse::class.java) return next(chain, type, context)

        val t: Type = p.actualTypeArguments.firstOrNull() ?: return next(chain, type, context)
        val tSchema = context.resolve(AnnotatedType(t))

        val schemaName =
            tSchema.name
                ?: (t as? Class<*>)?.simpleName
                ?: (t as? ParameterizedType)?.rawType.let { (it as? Class<*>)?.simpleName }
                ?: "Anonymous"

        val s =
            ObjectSchema().apply {
                name = schemaName
                addProperty("status", StringSchema())
                addProperty("message", StringSchema())
                addProperty("code", StringSchema())
                addProperty("data", tSchema)
            }

        return s
    }

    private fun unwrapKnownWrappers(type: Type): Type {
        var current = type
        while (current is ParameterizedType) {
            val raw = current.rawType as? Class<*>
            if (raw == ResponseEntity::class.java || raw == HttpEntity::class.java) {
                current = current.actualTypeArguments.firstOrNull() ?: return current
            } else {
                break
            }
        }
        return current
    }

    private fun next(
        chain: MutableIterator<ModelConverter>,
        type: AnnotatedType,
        context: ModelConverterContext,
    ): Schema<*>? = if (chain.hasNext()) chain.next().resolve(type, context, chain) else null
}
