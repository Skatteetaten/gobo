package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class AffiliationDataLoader : KeysDataLoader<String, Try<Affiliation>> {
    override fun getByKeys(user: User, keys: List<String>): List<Try<Affiliation>> =
        keys.map { Try.succeeded(Affiliation(it)) }
}
