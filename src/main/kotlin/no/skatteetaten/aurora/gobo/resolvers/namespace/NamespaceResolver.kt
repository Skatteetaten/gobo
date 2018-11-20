package no.skatteetaten.aurora.gobo.resolvers.namespace

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.permission.Permission
import org.springframework.stereotype.Component

@Component
class NamespaceResolver : GraphQLResolver<Namespace> {

    fun affiliation(namespace: Namespace, dfe: DataFetchingEnvironment) = Affiliation(namespace.affiliationId)

    fun permission(namespace: Namespace, dfe: DataFetchingEnvironment) =
        dfe.loader(Permission::class).load(namespace)
}