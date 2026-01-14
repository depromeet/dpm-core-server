package core.application.security.oauth.repository.mapper

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64

@Component
class AuthorizationRequestCookieValueMapper {
    private val objectMapper: ObjectMapper =
        ObjectMapper().apply {
            addMixIn(OAuth2AuthorizationRequest::class.java, OAuth2AuthorizationRequestMixin::class.java)
            addMixIn(OAuth2AuthorizationResponseType::class.java, OAuth2AuthorizationResponseTypeMixin::class.java)
            addMixIn(AuthorizationGrantType::class.java, AuthorizationGrantTypeMixin::class.java)
        }

    fun serialize(authorizationRequest: OAuth2AuthorizationRequest): String {
        return try {
            val json = objectMapper.writeValueAsString(authorizationRequest)
            Base64.getUrlEncoder().encodeToString(json.toByteArray(StandardCharsets.UTF_8))
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Failed to serialize OAuth2AuthorizationRequest", e)
        }
    }

    fun deserialize(value: String): OAuth2AuthorizationRequest {
        return try {
            val decoded = Base64.getUrlDecoder().decode(value)
            objectMapper.readValue(decoded, OAuth2AuthorizationRequest::class.java)
        } catch (e: IOException) {
            throw RuntimeException("Failed to deserialize OAuth2AuthorizationRequest", e)
        }
    }

    abstract class AuthorizationGrantTypeMixin
        @JsonCreator
        constructor(
            @JsonProperty("value") value: String,
        )

    abstract class OAuth2AuthorizationRequestMixin {
        @JsonProperty("grantType")
        lateinit var authorizationGrantType: AuthorizationGrantType
    }

    abstract class OAuth2AuthorizationResponseTypeMixin
        @JsonCreator
        constructor(
            @JsonProperty("value") value: String,
        )
}
