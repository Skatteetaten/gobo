package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.application.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import org.springframework.stereotype.Component

@Component
class AffiliationDataLoader(
    val applicationService: ApplicationService
) : KeysDataLoader<String, Affiliation> {
    override fun getByKeys(keys: List<String>): List<Affiliation> {
        return keys.map { Affiliation(it) }
    }
}
