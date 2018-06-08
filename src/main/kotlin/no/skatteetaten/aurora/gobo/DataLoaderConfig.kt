package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationDataLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataLoaderConfig {
    @Bean
    fun applicationDataLoaderNoCache(applicationDataLoader: ApplicationDataLoader) =
        NoCacheBatchDataLoader(applicationDataLoader)
}