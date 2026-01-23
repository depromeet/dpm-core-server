package core.application.security.redirect.strategy

import core.application.security.redirect.model.RedirectContext

class CompositeRedirectStrategy(
    private val strategies: List<RedirectStrategy>,
    private val errorRedirectStrategy: ErrorRedirectStrategy,
) : RedirectStrategy {
    override fun supports(context: RedirectContext): Boolean = true

    override fun resolve(context: RedirectContext): String {
        val strategy =
            strategies.firstOrNull { it.supports(context) }
                ?: return errorRedirectStrategy.resolve(
                    context.copy(
                        error = context.error ?: "No matching redirect strategy",
                    ),
                )

        return strategy.resolve(context)
    }
}
