package core.application.security.redirect.strategy

import core.application.security.redirect.model.RedirectContext

class CompositeRedirectStrategy(
    private val strategies: List<RedirectStrategy>,
) : RedirectStrategy {
    class NoMatchingStrategyException : RuntimeException()

    override fun supports(context: RedirectContext): Boolean = true

    override fun resolve(context: RedirectContext): String {
        val strategy =
            strategies.firstOrNull { it.supports(context) }
                ?: throw NoMatchingStrategyException()

        return strategy.resolve(context)
    }
}
