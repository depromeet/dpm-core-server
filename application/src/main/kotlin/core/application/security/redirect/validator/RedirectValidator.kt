package core.application.security.redirect.validator

import org.springframework.stereotype.Component
import java.net.URI

@Component
class RedirectValidator {
    class InvalidRedirectUrlException(url: String) :
        RuntimeException("Invalid redirect URL: $url")

    fun validate(url: String): String {
        val uri = URI(url)
        if (uri.path.contains("..")) throw InvalidRedirectUrlException(url)
        if (!url.startsWith("http://") && !url.startsWith("https://")) throw InvalidRedirectUrlException(url)
        return url
    }
}
