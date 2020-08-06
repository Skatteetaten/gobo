package no.skatteetaten.aurora.gobo

/*
@Component
class GoboGraphQLContextBuilder(
    val keyLoaders: List<KeyDataLoader<*, *>>,
    val multipleKeysDataLoaders: List<MultipleKeysDataLoader<*, *>>
) : GraphQLContextBuilder {

    override fun build(httpServletRequest: HttpServletRequest?, httpServletResponse: HttpServletResponse?) =
        createContext(httpServletRequest)

    override fun build(session: Session?, handshakeRequest: HandshakeRequest?) = createContext()

    override fun build() = throw UnsupportedOperationException()

    private fun createContext(request: HttpServletRequest? = null): DefaultGraphQLServletContext {
        val currentUser = request?.currentUser() ?: ANONYMOUS_USER
        val registry = DataLoaderRegistry().apply {
            keyLoaders.forEach {
                register(it::class.simpleName, batchDataLoaderMappedSingle(currentUser, it))
            }
            multipleKeysDataLoaders.forEach {
                register(it::class.simpleName, batchDataLoaderMappedMultiple(currentUser, it))
            }
        }

        return DefaultGraphQLServletContext.createServletContext().with(request).with(registry).build()
    }
}
*/
