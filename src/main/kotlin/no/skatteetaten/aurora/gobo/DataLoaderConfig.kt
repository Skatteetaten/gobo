package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import no.skatteetaten.aurora.gobo.resolvers.affiliation.AffiliationDataLoader
import no.skatteetaten.aurora.gobo.resolvers.namespace.NamespaceDataLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataLoaderConfig {
    @Bean
    fun affiliationDataLoaderNoCache(affiliationDataLoader: AffiliationDataLoader) =
        NoCacheBatchDataLoader(affiliationDataLoader)

    @Bean
    fun namespaceDataLoaderNoCache(namespaceDataLoader: NamespaceDataLoader) =
        NoCacheBatchDataLoader(namespaceDataLoader)
}