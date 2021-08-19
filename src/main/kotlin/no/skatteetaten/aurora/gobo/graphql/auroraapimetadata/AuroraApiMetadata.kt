package no.skatteetaten.aurora.gobo.graphql.auroraapimetadata

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadValue

data class AuroraApiMetadata(
    val clientConfig: ClientConfig
) {
    @Deprecated(
        "Please use affiliations instead",
        ReplaceWith("{affiliations(includeUndeployed: true){ edges { node { name }}}}")
    )
    fun configNames(dfe: DataFetchingEnvironment) =
        dfe.loadValue<AuroraApiMetadata, List<String>>(key = this, loaderClass = ConfigNamesDataLoader::class)
}

data class ClientConfig(
    val gitUrlPattern: String,
    val openshiftCluster: String,
    val openshiftUrl: String,
    val apiVersion: Int
)
