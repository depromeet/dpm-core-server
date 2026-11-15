package core.application.common.converter

import core.application.common.exception.CustomResponse
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
        val isVoid = (t as? Class<*>)?.let { it == Void::class.java || it == Void.TYPE } == true

        val dataProperty: Schema<*>? =
            if (isVoid) {
                null
            } else {
                val resolved = context.resolve(AnnotatedType(t))
                val componentName = resolved?.name ?: fallbackName(t)
                if (resolved != null && componentName != null && resolved.`$ref` == null) {
                    // context.resolve 가 전체 스키마를 반환할 때가 있다. components 로 등록해야 data 가 $ref 로 참조된다.
                    context.defineModel(componentName, resolved)
                    Schema<Any>().`$ref`("#/components/schemas/$componentName")
                } else {
                    resolved
                }
            }

        // Wrapper must not reuse the data DTO name (e.g. SessionListResponse), or OpenAPI
        // components overwrite the DTO and clients bind success types to the wrong status codes.
        val dataSchemaName =
            if (isVoid) {
                "Void"
            } else {
                dataProperty?.`$ref`?.substringAfterLast('/')
                    ?: fallbackName(t)
                    ?: "Anonymous"
            }
        val wrapperSchemaName = "CustomResponse$dataSchemaName"

        return ObjectSchema().apply {
            name = wrapperSchemaName
            addProperty("status", StringSchema())
            addProperty("message", StringSchema())
            addProperty("code", StringSchema())
            if (!isVoid && dataProperty != null) {
                addProperty("data", dataProperty)
                required = listOf("status", "message", "code", "data")
            } else {
                required = listOf("status", "message", "code")
            }
        }
    }

    private fun fallbackName(t: Type): String? =
        (t as? Class<*>)?.simpleName
            ?: ((t as? ParameterizedType)?.rawType as? Class<*>)?.simpleName

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
