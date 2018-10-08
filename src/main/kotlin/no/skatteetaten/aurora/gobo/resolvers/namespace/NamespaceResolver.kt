package no.skatteetaten.aurora.gobo.resolvers.namespace

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.loader
import org.springframework.stereotype.Component

@Component
class NamespaceResolver : GraphQLResolver<Namespace> {

    fun affiliation(namespace: Namespace, dfe: DataFetchingEnvironment) =
        dfe.loader(Affiliation::class).load(namespace.affiliationId)
}