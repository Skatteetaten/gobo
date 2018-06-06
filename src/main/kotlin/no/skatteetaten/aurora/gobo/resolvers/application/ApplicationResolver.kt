package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class ApplicationResolver(
    val applicationDataLoader: ApplicationDataLoader
) : GraphQLResolver<Application> {
    fun namespace(application: Application): Namespace =
        Namespace(
            application.name,
            Affiliation("", ApplicationsConnection(emptyList(), null)), ApplicationsConnection(
                emptyList(), null
            )
        )

    fun affiliation(application: Application): CompletableFuture<Affiliation>? =
        applicationDataLoader.load(application.affiliation)
}