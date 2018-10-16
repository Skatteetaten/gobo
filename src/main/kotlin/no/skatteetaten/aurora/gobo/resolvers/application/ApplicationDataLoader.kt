package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoaderFlux
import org.springframework.stereotype.Component

@Component
class ApplicationDataLoader(private val applicationService: ApplicationService) : KeysDataLoader<> {


}