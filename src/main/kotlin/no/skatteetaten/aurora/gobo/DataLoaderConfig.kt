package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoaderFlux
import no.skatteetaten.aurora.gobo.resolvers.affiliation.AffiliationDataLoader
import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.ApplicationDeploymentDetailsDataLoader
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.TagDataLoader
import no.skatteetaten.aurora.gobo.resolvers.namespace.NamespaceDataLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataLoaderConfig {

    @Bean
    fun affiliationDataLoaderNoCache(loader: AffiliationDataLoader) = NoCacheBatchDataLoader(loader)

    @Bean
    fun namespaceDataLoaderNoCache(loader: NamespaceDataLoader) = NoCacheBatchDataLoader(loader)

    @Bean
    fun tagDataLoaderNoCache(loader: TagDataLoader) = NoCacheBatchDataLoader(loader)

    @Bean
    fun applicationDeploymentDetailsDataLoaderNoCache(loader: ApplicationDeploymentDetailsDataLoader) =
        NoCacheBatchDataLoaderFlux(loader)
}