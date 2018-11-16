package no.skatteetaten.aurora.gobo.resolvers.userSettings

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class UserSettingsQueryResolver(private val userSettingsService: UserSettingsService) :
    GraphQLQueryResolver {

    fun getUserSettings(dfe: DataFetchingEnvironment): UserSettings {
        val settings = userSettingsService.getUserSettings(dfe.currentUser().token)
        return UserSettings.create(settings)
    }
}