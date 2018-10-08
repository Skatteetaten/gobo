package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class AffiliationDataLoader : KeysDataLoader<String, Try<Affiliation>> {
    override fun getByKeys(keys: List<String>): List<Try<Affiliation>> =
        keys.map { Try.succeeded(Affiliation(it)) }
}
