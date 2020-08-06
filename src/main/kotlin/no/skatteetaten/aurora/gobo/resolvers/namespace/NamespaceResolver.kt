package no.skatteetaten.aurora.gobo.resolvers.namespace

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.load
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.permission.PermissionDataLoader
import org.springframework.stereotype.Component

@Component
class NamespaceResolver : Query {

    fun affiliation(namespace: Namespace, dfe: DataFetchingEnvironment) = Affiliation(namespace.affiliationId)

    suspend fun permission(namespace: Namespace, dfe: DataFetchingEnvironment) =
        dfe.load<Namespace, PermissionDataLoader>(namespace)
}
