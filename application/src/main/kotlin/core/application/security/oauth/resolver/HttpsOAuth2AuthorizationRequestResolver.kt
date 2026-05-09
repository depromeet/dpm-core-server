package core.application.security.oauth.resolver

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class HttpsOAuth2AuthorizationRequestResolver(
    clientRegistrationRepository: ClientRegistrationRepository,
) : OAuth2AuthorizationRequestResolver {
    private val delegate =
        DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository,
            AUTHORIZATION_REQUEST_BASE_URI,
        )

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        delegate.resolve(request)?.withExternalHttpsRedirectUri()

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String,
    ): OAuth2AuthorizationRequest? = delegate.resolve(request, clientRegistrationId)?.withExternalHttpsRedirectUri()

    private fun OAuth2AuthorizationRequest.withExternalHttpsRedirectUri(): OAuth2AuthorizationRequest {
        val normalizedRedirectUri = redirectUri.toExternalHttpsUri()
        if (normalizedRedirectUri == redirectUri) {
            return this
        }

        return OAuth2AuthorizationRequest
            .from(this)
            .redirectUri(normalizedRedirectUri)
            .authorizationRequestUri(buildAuthorizationRequestUri(normalizedRedirectUri))
            .build()
    }

    private fun String.toExternalHttpsUri(): String {
        val uri = URI.create(this)
        if (uri.scheme != "http" || uri.isLocalhost()) {
            return this
        }

        return UriComponentsBuilder
            .fromUri(uri)
            .scheme("https")
            .build()
            .toUriString()
    }

    private fun URI.isLocalhost(): Boolean =
        host == "localhost" ||
            host == "127.0.0.1" ||
            host == "::1"

    private fun OAuth2AuthorizationRequest.buildAuthorizationRequestUri(redirectUri: String): String {
        val builder =
            UriComponentsBuilder
                .fromUriString(authorizationUri)
                .queryParam(OAuth2ParameterNames.RESPONSE_TYPE, responseType.value)
                .queryParam(OAuth2ParameterNames.CLIENT_ID, clientId)
                .queryParam(OAuth2ParameterNames.SCOPE, scopes.joinToString(" "))
                .queryParam(OAuth2ParameterNames.STATE, state)
                .queryParam(OAuth2ParameterNames.REDIRECT_URI, redirectUri)

        additionalParameters
            .filterKeys { it !in STANDARD_AUTHORIZATION_PARAMETERS }
            .forEach { (name, value) -> builder.queryParam(name, value) }

        return builder
            .build()
            .encode()
            .toUriString()
    }

    companion object {
        private const val AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization"

        private val STANDARD_AUTHORIZATION_PARAMETERS =
            setOf(
                OAuth2ParameterNames.RESPONSE_TYPE,
                OAuth2ParameterNames.CLIENT_ID,
                OAuth2ParameterNames.SCOPE,
                OAuth2ParameterNames.STATE,
                OAuth2ParameterNames.REDIRECT_URI,
            )
    }
}
